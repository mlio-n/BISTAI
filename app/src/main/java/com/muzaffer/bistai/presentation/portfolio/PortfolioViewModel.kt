package com.muzaffer.bistai.presentation.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muzaffer.bistai.core.Resource
import com.muzaffer.bistai.domain.model.ChartData
import com.muzaffer.bistai.domain.model.ChartPoint
import com.muzaffer.bistai.domain.model.ChartTimeRange
import com.muzaffer.bistai.domain.model.PortfolioItem
import com.muzaffer.bistai.domain.model.PortfolioQuickAction
import com.muzaffer.bistai.domain.model.Stock
import com.muzaffer.bistai.domain.repository.AnalysisRepository
import com.muzaffer.bistai.domain.repository.AuthRepository
import com.muzaffer.bistai.domain.repository.NewsRepository
import com.muzaffer.bistai.domain.repository.PortfolioRepository
import com.muzaffer.bistai.domain.repository.StockRepository
import com.muzaffer.bistai.domain.repository.WatchlistRepository
import com.muzaffer.bistai.domain.usecase.GetStocksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

data class PortfolioUiState(
    val stocks: List<Stock>                    = emptyList(),
    val isLoading: Boolean                     = false,
    val errorMessage: String?                  = null,
    val favoriteSymbols: Set<String>           = emptySet(),
    val portfolioItems: List<PortfolioItem>    = emptyList(),
    val isAuthenticated: Boolean               = false,
    val selectedQuickAction: PortfolioQuickAction? = null,
    val isQuickActionSheetVisible: Boolean     = false,
    val isQuickActionLoading: Boolean          = false,
    val quickActionResult: String?             = null,
    val quickActionError: String?              = null,
    val selectedChartRange: ChartTimeRange     = ChartTimeRange.ONE_MONTH,
    val portfolioChartData: ChartData?         = null,
    val isChartLoading: Boolean                = false
) {
    val hasError: Boolean get() = errorMessage != null
    val isEmpty: Boolean  get() = !isLoading && stocks.isEmpty() && !hasError

    val totalInvestment: Double
        get() = portfolioItems.sumOf { it.totalInvestment }

    val currentTotalValue: Double
        get() = portfolioItems.sumOf { item ->
            val stock = stocks.find { it.symbol == item.symbol }
            val currentPrice = stock?.currentPrice ?: item.averageCost
            currentPrice * item.lotSize
        }

    val totalProfitLoss: Double
        get() = currentTotalValue - totalInvestment

    val totalProfitLossPercentage: Double
        get() = if (totalInvestment > 0) (totalProfitLoss / totalInvestment) * 100 else 0.0
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val getStocksUseCase: GetStocksUseCase,
    private val stockRepository: StockRepository,
    private val watchlistRepository: WatchlistRepository,
    private val authRepository: AuthRepository,
    private val portfolioRepository: PortfolioRepository,
    private val analysisRepository: AnalysisRepository,
    private val newsRepository: NewsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PortfolioUiState())
    val uiState: StateFlow<PortfolioUiState> = _uiState.asStateFlow()

    private var quickActionJob: Job? = null
    private var chartJob: Job? = null
    private var lastLoadedPortfolioKey: List<Pair<String, Double>>? = null

    init {
        loadStocks()
        observeFavorites()
        observeAuthAndPortfolio()
    }

    fun loadStocks() {
        viewModelScope.launch {
            getStocksUseCase()
                .onEach { resource ->
                    _uiState.update { current ->
                        when (resource) {
                            is Resource.Loading -> current.copy(
                                isLoading = true,
                                errorMessage = null
                            )
                            is Resource.Success -> current.copy(
                                isLoading = false,
                                stocks = resource.data,
                                errorMessage = null
                            )
                            is Resource.Error -> current.copy(
                                isLoading = false,
                                errorMessage = resource.message
                            )
                        }
                    }
                }
                .launchIn(this)
        }
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            watchlistRepository.getAllFavorites().collect { list ->
                _uiState.update { it.copy(favoriteSymbols = list.map { e -> e.symbol }.toSet()) }
            }
        }
    }

    private fun observeAuthAndPortfolio() {
        viewModelScope.launch {
            authRepository.getAuthState()
                .flatMapLatest { user ->
                    _uiState.update { it.copy(isAuthenticated = user != null) }
                    portfolioRepository.getPortfolio(user?.uid)
                }
                .collect { items ->
                    _uiState.update { it.copy(portfolioItems = items) }
                    val key = items.filter { it.lotSize > 0 }.map { it.symbol to it.lotSize }
                    if (lastLoadedPortfolioKey != key) {
                        lastLoadedPortfolioKey = key
                        loadPortfolioChart(_uiState.value.selectedChartRange, items)
                    }
                }
        }
    }

    fun selectChartRange(range: ChartTimeRange) {
        _uiState.update { it.copy(selectedChartRange = range) }
        loadPortfolioChart(range, _uiState.value.portfolioItems)
    }

    private fun loadPortfolioChart(range: ChartTimeRange, items: List<PortfolioItem>) {
        chartJob?.cancel()
        chartJob = viewModelScope.launch {
            _uiState.update { it.copy(isChartLoading = true) }
            val owned = items.filter { it.lotSize > 0 }

            val resultChart: ChartData? = when {
                owned.isEmpty() -> {
                    val res = runCatching {
                        stockRepository.getStockChart("THYAO", range)
                            .filter { it !is Resource.Loading }
                            .firstOrNull()
                    }.getOrNull() as? Resource.Success
                    res?.data
                }
                owned.size == 1 -> {
                    val item = owned.first()
                    val res = runCatching {
                        stockRepository.getStockChart(item.symbol, range)
                            .filter { it !is Resource.Loading }
                            .firstOrNull()
                    }.getOrNull() as? Resource.Success
                    res?.data?.let { raw ->
                        val scaledPoints = raw.points.map {
                            it.copy(closePrice = it.closePrice * item.lotSize)
                        }
                        val liveTotal = _uiState.value.currentTotalValue
                            .takeIf { it > 0.0 }
                            ?: (raw.lastPrice * item.lotSize)
                        ChartData(
                            symbol = "PORTFÖY",
                            range = range,
                            points = scaledPoints,
                            currentPrice = liveTotal,
                            previousClose = raw.previousClose?.times(item.lotSize)
                        )
                    }
                }
                else -> {
                    val fetched = coroutineScope {
                        owned.map { item ->
                            async {
                                val res = runCatching {
                                    stockRepository.getStockChart(item.symbol, range)
                                        .filter { it !is Resource.Loading }
                                        .firstOrNull()
                                }.getOrNull() as? Resource.Success
                                item to res?.data
                            }
                        }.awaitAll()
                    }
                    val validPairs = fetched.mapNotNull { (item, chart) ->
                        chart?.takeIf { it.points.isNotEmpty() }?.let { item to it }
                    }
                    val baseChart = validPairs.maxByOrNull { it.second.points.size }?.second
                    if (baseChart != null) {
                        val combinedPoints = baseChart.points.map { basePoint ->
                            val sumValue = validPairs.sumOf { (item, chart) ->
                                val nearest = chart.points.minByOrNull { abs(it.timestamp - basePoint.timestamp) }
                                (nearest?.closePrice ?: item.averageCost) * item.lotSize
                            }
                            ChartPoint(timestamp = basePoint.timestamp, closePrice = sumValue)
                        }
                        val liveTotal = _uiState.value.currentTotalValue
                            .takeIf { it > 0.0 }
                            ?: combinedPoints.lastOrNull()?.closePrice
                            ?: 0.0
                        ChartData(
                            symbol = "PORTFÖY",
                            range = range,
                            points = combinedPoints,
                            currentPrice = liveTotal,
                            previousClose = null
                        )
                    } else {
                        null
                    }
                }
            }

            _uiState.update {
                it.copy(
                    isChartLoading = false,
                    portfolioChartData = resultChart ?: it.portfolioChartData
                )
            }
        }
    }

    fun toggleFavorite(symbol: String, name: String) {
        viewModelScope.launch {
            watchlistRepository.toggleFavorite(symbol, name)
        }
    }

    fun executeQuickAction(action: PortfolioQuickAction) {
        quickActionJob?.cancel()
        _uiState.update {
            it.copy(
                selectedQuickAction = action,
                isQuickActionSheetVisible = true,
                isQuickActionLoading = true,
                quickActionResult = null,
                quickActionError = null
            )
        }

        quickActionJob = viewModelScope.launch {
            val snapshot = _uiState.value
            val news = newsRepository.getNewsForAsset("BIST100")
            val macro = newsRepository.getMacroContext()

            analysisRepository.runPortfolioQuickAction(
                action = action,
                portfolioItems = snapshot.portfolioItems,
                stocks = snapshot.stocks,
                newsItems = news,
                macroContext = macro
            ).onSuccess { markdown ->
                _uiState.update {
                    it.copy(
                        isQuickActionLoading = false,
                        quickActionResult = markdown,
                        quickActionError = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isQuickActionLoading = false,
                        quickActionError = mapAiError(error)
                    )
                }
            }
        }
    }

    fun dismissQuickActionSheet() {
        quickActionJob?.cancel()
        _uiState.update {
            it.copy(
                isQuickActionSheetVisible = false,
                isQuickActionLoading = false
            )
        }
    }

    private fun mapAiError(error: Throwable): String = when {
        error.message == "API_KEY_MISSING" ->
            "API anahtarı tanımlı değil. Profil sekmesinden kendi API anahtarınızı kaydedebilir veya local.properties üzerinden GEMINI_API_KEY tanımlayabilirsiniz."
        error.message?.startsWith("API_KEY_INVALID") == true ||
        error.message?.startsWith("API_MODEL_NOT_FOUND") == true ->
            "${error.message}\n\naistudio.google.com adresinden geçerli bir anahtar alabilirsiniz."
        error.message?.startsWith("QUOTA_EXCEEDED") == true ->
            "Günlük kota aşıldı. Lütfen birkaç dakika bekleyin."
        error.message?.contains("Unable to resolve host") == true ||
        error.message?.contains("timeout") == true ->
            "İnternet bağlantısı kurulamadı."
        else -> error.message ?: "Analiz şu an yapılamıyor."
    }

    fun refresh() {
        loadStocks()
        loadPortfolioChart(_uiState.value.selectedChartRange, _uiState.value.portfolioItems)
    }
}

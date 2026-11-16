package com.muzaffer.bistai.presentation.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muzaffer.bistai.domain.model.Stock
import com.muzaffer.bistai.domain.repository.WatchlistRepository
import com.muzaffer.bistai.domain.usecase.GetStocksUseCase
import com.muzaffer.bistai.core.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.muzaffer.bistai.domain.model.PortfolioItem
import com.muzaffer.bistai.domain.model.PortfolioQuickAction
import com.muzaffer.bistai.domain.repository.AnalysisRepository
import com.muzaffer.bistai.domain.repository.AuthRepository
import com.muzaffer.bistai.domain.repository.NewsRepository
import com.muzaffer.bistai.domain.repository.PortfolioRepository
import kotlinx.coroutines.Job

// ─── UI State ────────────────────────────────────────────────────────────────

data class PortfolioUiState(
    val stocks: List<Stock>                    = emptyList(),
    val isLoading: Boolean                     = false,
    val errorMessage: String?                  = null,
    val favoriteSymbols: Set<String>           = emptySet(),
    val portfolioItems: List<PortfolioItem>    = emptyList(), // Kullanıcının cüzdanı
    val isAuthenticated: Boolean               = false,
    val selectedQuickAction: PortfolioQuickAction? = null,
    val isQuickActionSheetVisible: Boolean     = false,
    val isQuickActionLoading: Boolean          = false,
    val quickActionResult: String?             = null,
    val quickActionError: String?              = null
) {
    val hasError: Boolean get() = errorMessage != null
    val isEmpty: Boolean  get() = !isLoading && stocks.isEmpty() && !hasError
    
    // Toplam Yatırım (Maliyet * Lot)
    val totalInvestment: Double
        get() = portfolioItems.sumOf { it.totalInvestment }
        
    // Güncel Varlık Değeri (Anlık Fiyat * Lot)
    val currentTotalValue: Double
        get() = portfolioItems.sumOf { item ->
            val stock = stocks.find { it.symbol == item.symbol }
            val currentPrice = stock?.currentPrice ?: item.averageCost
            currentPrice * item.lotSize
        }
        
    // Toplam Kâr / Zarar
    val totalProfitLoss: Double
        get() = currentTotalValue - totalInvestment
    
    // Yüzdelik Kâr / Zarar
    val totalProfitLossPercentage: Double
        get() = if (totalInvestment > 0) (totalProfitLoss / totalInvestment) * 100 else 0.0
}

// ─── ViewModel ───────────────────────────────────────────────────────────────

@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val getStocksUseCase: GetStocksUseCase,
    private val watchlistRepository: WatchlistRepository,
    private val authRepository: AuthRepository,
    private val portfolioRepository: PortfolioRepository,
    private val analysisRepository: AnalysisRepository,
    private val newsRepository: NewsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PortfolioUiState())
    val uiState: StateFlow<PortfolioUiState> = _uiState.asStateFlow()

    private var quickActionJob: Job? = null

    init {
        loadStocks()
        observeFavorites()
        observeAuthAndPortfolio()
    }

    /** Hisse listesini yükler. */
    fun loadStocks() {
        viewModelScope.launch {
            getStocksUseCase()
                .onEach { resource ->
                    _uiState.update { current ->
                        when (resource) {
                            is Resource.Loading -> current.copy(
                                isLoading    = true,
                                errorMessage = null
                            )
                            is Resource.Success -> current.copy(
                                isLoading    = false,
                                stocks       = resource.data,
                                errorMessage = null
                            )
                            is Resource.Error   -> current.copy(
                                isLoading    = false,
                                errorMessage = resource.message
                            )
                        }
                    }
                }
                .launchIn(this)
        }
    }

    /** Favori semboller Flow'unu dinler, state'i günceller. */
    private fun observeFavorites() {
        viewModelScope.launch {
            watchlistRepository.getAllFavorites().collect { list ->
                _uiState.update { it.copy(favoriteSymbols = list.map { e -> e.symbol }.toSet()) }
            }
        }
    }

    /** Kullanıcı girişini ve yerel/bulut cüzdanını dinler. Misafir modunda yerel Room verisini kullanır. */
    private fun observeAuthAndPortfolio() {
        viewModelScope.launch {
            authRepository.getAuthState().collectLatest { user ->
                _uiState.update { it.copy(isAuthenticated = user != null) }

                if (user != null) {
                    portfolioRepository.syncLocalToCloud(user.uid)
                    portfolioRepository.getPortfolio(user.uid).collect { items ->
                        _uiState.update { it.copy(portfolioItems = items) }
                    }
                } else {
                    portfolioRepository.getPortfolio(null).collect { items ->
                        _uiState.update { it.copy(portfolioItems = items) }
                    }
                }
            }
        }
    }

    /** Hisseyi favoriye ekle / çıkar. */
    fun toggleFavorite(symbol: String, name: String) {
        viewModelScope.launch {
            watchlistRepository.toggleFavorite(symbol, name)
        }
    }

    fun executeQuickAction(action: PortfolioQuickAction) {
        quickActionJob?.cancel()
        _uiState.update {
            it.copy(
                selectedQuickAction       = action,
                isQuickActionSheetVisible = true,
                isQuickActionLoading      = true,
                quickActionResult         = null,
                quickActionError          = null
            )
        }

        quickActionJob = viewModelScope.launch {
            val snapshot = _uiState.value
            val news = newsRepository.getNewsForAsset("BIST100")
            val macro = newsRepository.getMacroContext()

            analysisRepository.runPortfolioQuickAction(
                action         = action,
                portfolioItems = snapshot.portfolioItems,
                stocks         = snapshot.stocks,
                newsItems      = news,
                macroContext   = macro
            ).onSuccess { markdown ->
                _uiState.update {
                    it.copy(
                        isQuickActionLoading = false,
                        quickActionResult    = markdown,
                        quickActionError     = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isQuickActionLoading = false,
                        quickActionError     = mapAiError(error)
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
                isQuickActionLoading      = false
            )
        }
    }

    private fun mapAiError(error: Throwable): String = when {
        error.message == "API_KEY_MISSING" ->
            "API anahtarı tanımlı değil. Menü sekmesinden kendi API anahtarınızı kaydedebilir veya local.properties üzerinden GEMINI_API_KEY tanımlayabilirsiniz."
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

    fun refresh() = loadStocks()
}

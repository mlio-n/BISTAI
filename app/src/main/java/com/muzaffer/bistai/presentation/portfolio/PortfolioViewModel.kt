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
import com.muzaffer.bistai.domain.repository.AuthRepository
import com.muzaffer.bistai.domain.repository.PortfolioRepository

// ─── UI State ────────────────────────────────────────────────────────────────

data class PortfolioUiState(
    val stocks: List<Stock>          = emptyList(),
    val isLoading: Boolean           = false,
    val errorMessage: String?        = null,
    val favoriteSymbols: Set<String> = emptySet(),
    val portfolioItems: List<PortfolioItem> = emptyList(), // Kullanıcının cüzdanı
    val isAuthenticated: Boolean     = false
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
    private val portfolioRepository: PortfolioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PortfolioUiState())
    val uiState: StateFlow<PortfolioUiState> = _uiState.asStateFlow()

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

    /** Kullanıcı girişini ve cüzdanını dinler. */
    private fun observeAuthAndPortfolio() {
        viewModelScope.launch {
            authRepository.getAuthState().collectLatest { user ->
                _uiState.update { it.copy(isAuthenticated = user != null) }
                
                if (user != null) {
                    portfolioRepository.getPortfolio(user.uid).collect { items ->
                        _uiState.update { it.copy(portfolioItems = items) }
                    }
                } else {
                    _uiState.update { it.copy(portfolioItems = emptyList()) }
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

    fun refresh() = loadStocks()
}

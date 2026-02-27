package com.muzaffer.bistai.presentation.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muzaffer.bistai.domain.model.Stock
import com.muzaffer.bistai.domain.repository.WatchlistRepository
import com.muzaffer.bistai.domain.usecase.GetStocksUseCase
import com.muzaffer.bistai.domain.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── UI State ────────────────────────────────────────────────────────────────

data class PortfolioUiState(
    val stocks: List<Stock>         = emptyList(),
    val isLoading: Boolean          = false,
    val errorMessage: String?       = null,
    val favoriteSymbols: Set<String> = emptySet(),
    val showOnlyFavorites: Boolean  = false
) {
    val hasError: Boolean get() = errorMessage != null
    val isEmpty: Boolean  get() = !isLoading && stocks.isEmpty() && !hasError
}

// ─── ViewModel ───────────────────────────────────────────────────────────────

@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val getStocksUseCase: GetStocksUseCase,
    private val watchlistRepository: WatchlistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PortfolioUiState())
    val uiState: StateFlow<PortfolioUiState> = _uiState.asStateFlow()

    init {
        loadStocks()
        observeFavorites()
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

    /** Hisseyi favoriye ekle / çıkar. */
    fun toggleFavorite(symbol: String, name: String) {
        viewModelScope.launch {
            watchlistRepository.toggleFavorite(symbol, name)
        }
    }

    /** Sadece favorileri göster / tümünü göster toggle. */
    fun toggleFavoriteFilter() {
        _uiState.update { it.copy(showOnlyFavorites = !it.showOnlyFavorites) }
    }

    fun refresh() = loadStocks()
}

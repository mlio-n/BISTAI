package com.muzaffer.bistai.presentation.stockdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muzaffer.bistai.data.remote.AiApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StockDetailUiState(
    val symbol: String   = "",
    val isLoading: Boolean = false,
    val analysis: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class StockDetailViewModel @Inject constructor(
    private val aiService: AiApiService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(StockDetailUiState())
    val uiState: StateFlow<StockDetailUiState> = _uiState.asStateFlow()

    init {
        val symbol = savedStateHandle.get<String>("symbol") ?: ""
        _uiState.update { it.copy(symbol = symbol) }
        fetchAnalysis(symbol)
    }

    fun fetchAnalysis(symbol: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, analysis = null) }
            aiService.analyzeStock(symbol)
                .onSuccess { text ->
                    _uiState.update { it.copy(isLoading = false, analysis = text) }
                }
                .onFailure { error ->
                    val msg = when {
                        error.message == "API_KEY_MISSING"  -> "Gemini API anahtarı tanımlı değil."
                        error.message?.contains("network", ignoreCase = true) == true ->
                            "İnternet bağlantısı kurulamadı."
                        else -> "Analiz şu an yapılamıyor. Lütfen daha sonra tekrar deneyin."
                    }
                    _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
                }
        }
    }
}

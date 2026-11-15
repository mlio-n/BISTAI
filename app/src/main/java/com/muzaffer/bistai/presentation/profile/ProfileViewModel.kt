package com.muzaffer.bistai.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muzaffer.bistai.data.local.prefs.ApiKeyManager
import com.muzaffer.bistai.domain.model.User
import com.muzaffer.bistai.domain.repository.AuthRepository
import com.muzaffer.bistai.domain.repository.PortfolioRepository
import com.muzaffer.bistai.domain.repository.WatchlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user: User? = null,
    val apiKeyInput: String = "",
    val isCustomApiKeySaved: Boolean = false,
    val portfolioCount: Int = 0,
    val favoritesCount: Int = 0,
    val isSyncing: Boolean = false,
    val statusMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val portfolioRepository: PortfolioRepository,
    private val watchlistRepository: WatchlistRepository,
    private val apiKeyManager: ApiKeyManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ProfileUiState(
            apiKeyInput = apiKeyManager.getUserApiKey(),
            isCustomApiKeySaved = apiKeyManager.getUserApiKey().isNotBlank()
        )
    )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        observeUserAndCounts()
        observeApiKey()
    }

    private fun observeUserAndCounts() {
        viewModelScope.launch {
            authRepository.getAuthState()
                .flatMapLatest { user ->
                    _uiState.update { it.copy(user = user) }
                    portfolioRepository.getPortfolio(user?.uid)
                }
                .collect { items ->
                    _uiState.update { it.copy(portfolioCount = items.size) }
                }
        }

        viewModelScope.launch {
            watchlistRepository.getAllFavorites().collect { favorites ->
                _uiState.update { it.copy(favoritesCount = favorites.size) }
            }
        }
    }

    private fun observeApiKey() {
        viewModelScope.launch {
            apiKeyManager.userApiKey.collect { key ->
                _uiState.update {
                    it.copy(
                        apiKeyInput = key,
                        isCustomApiKeySaved = key.isNotBlank()
                    )
                }
            }
        }
    }

    fun onApiKeyInputChange(newValue: String) {
        _uiState.update { it.copy(apiKeyInput = newValue, statusMessage = null) }
    }

    fun saveApiKey() {
        val key = _uiState.value.apiKeyInput.trim()
        if (key.isBlank()) {
            _uiState.update { it.copy(statusMessage = "Lütfen geçerli bir Gemini API anahtarı girin.") }
            return
        }
        apiKeyManager.saveApiKey(key)
        _uiState.update {
            it.copy(
                isCustomApiKeySaved = true,
                statusMessage = "API anahtarı EncryptedSharedPreferences ile yerel olarak kaydedildi."
            )
        }
    }

    fun clearApiKey() {
        apiKeyManager.clearApiKey()
        _uiState.update {
            it.copy(
                apiKeyInput = "",
                isCustomApiKeySaved = false,
                statusMessage = "Özel API anahtarı temizlendi."
            )
        }
    }

    fun backupToCloud() {
        val user = _uiState.value.user ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, statusMessage = null) }
            val result = portfolioRepository.syncLocalToCloud(user.uid)
            _uiState.update {
                it.copy(
                    isSyncing = false,
                    statusMessage = if (result.isSuccess) {
                        "Yerel portföy verileriniz buluta yedeklendi."
                    } else {
                        result.exceptionOrNull()?.localizedMessage ?: "Bulut yedekleme başarısız oldu."
                    }
                )
            }
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}

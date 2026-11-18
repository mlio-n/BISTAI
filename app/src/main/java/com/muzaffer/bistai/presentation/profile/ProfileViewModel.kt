package com.muzaffer.bistai.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muzaffer.bistai.data.local.prefs.ApiKeyManager
import com.muzaffer.bistai.data.local.prefs.ThemePreferencesManager
import com.muzaffer.bistai.domain.model.AppThemeMode
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
    val lastSyncedAtMillis: Long = System.currentTimeMillis(),
    val statusMessage: String? = null,
    val emailInput: String = "",
    val passwordInput: String = "",
    val isAuthLoading: Boolean = false,
    val authErrorMessage: String? = null,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val portfolioRepository: PortfolioRepository,
    private val watchlistRepository: WatchlistRepository,
    private val apiKeyManager: ApiKeyManager,
    private val themePreferencesManager: ThemePreferencesManager
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
        observeThemeMode()
    }

    private fun observeUserAndCounts() {
        viewModelScope.launch {
            authRepository.getAuthState()
                .flatMapLatest { user ->
                    _uiState.update {
                        if (user != null) {
                            it.copy(
                                user = user,
                                passwordInput = "",
                                isAuthLoading = false,
                                authErrorMessage = null
                            )
                        } else {
                            it.copy(user = null)
                        }
                    }
                    portfolioRepository.getPortfolio(user?.uid)
                }
                .collect { items ->
                    _uiState.update {
                        it.copy(
                            portfolioCount = items.count { item -> item.lotSize > 0 },
                            lastSyncedAtMillis = if (it.user != null) System.currentTimeMillis() else it.lastSyncedAtMillis
                        )
                    }
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

    private fun observeThemeMode() {
        viewModelScope.launch {
            themePreferencesManager.themeMode.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            themePreferencesManager.setThemeMode(mode)
        }
    }

    fun onEmailInputChange(newValue: String) {
        _uiState.update { it.copy(emailInput = newValue, authErrorMessage = null) }
    }

    fun onPasswordInputChange(newValue: String) {
        _uiState.update { it.copy(passwordInput = newValue, authErrorMessage = null) }
    }

    fun signInWithEmail() {
        executeEmailAuth(isSignUp = false)
    }

    fun signUpWithEmail() {
        executeEmailAuth(isSignUp = true)
    }

    private fun executeEmailAuth(isSignUp: Boolean) {
        val email = _uiState.value.emailInput.trim()
        val password = _uiState.value.passwordInput

        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(authErrorMessage = "Lütfen e-posta ve şifre alanlarını doldurun.") }
            return
        }
        if (isSignUp && password.length < 6) {
            _uiState.update { it.copy(authErrorMessage = "Şifre en az 6 karakter olmalıdır.") }
            return
        }

        _uiState.update { it.copy(isAuthLoading = true, authErrorMessage = null) }

        viewModelScope.launch {
            val result = if (isSignUp) {
                authRepository.signUpWithEmail(email, password)
            } else {
                authRepository.signInWithEmail(email, password)
            }

            result.onSuccess { user ->
                portfolioRepository.syncLocalToCloud(user.uid)
                _uiState.update {
                    it.copy(
                        user = user,
                        isAuthLoading = false,
                        passwordInput = "",
                        authErrorMessage = null,
                        lastSyncedAtMillis = System.currentTimeMillis()
                    )
                }
            }.onFailure { error ->
                val fallback = if (isSignUp) "Kayıt başarısız oldu." else "Giriş başarısız oldu."
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        authErrorMessage = error.localizedMessage ?: fallback
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
                statusMessage = "Anahtarınız yalnızca bu cihazda şifreli saklanır."
            )
        }
    }

    fun clearApiKey() {
        apiKeyManager.clearApiKey()
        _uiState.update {
            it.copy(
                apiKeyInput = "",
                isCustomApiKeySaved = false,
                statusMessage = "Özel API anahtarı kaldırıldı."
            )
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}

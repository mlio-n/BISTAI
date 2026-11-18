package com.muzaffer.bistai.presentation.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muzaffer.bistai.data.local.prefs.ThemePreferencesManager
import com.muzaffer.bistai.domain.model.AppThemeMode
import com.muzaffer.bistai.domain.repository.AuthRepository
import com.muzaffer.bistai.domain.repository.PortfolioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    themePreferencesManager: ThemePreferencesManager,
    authRepository: AuthRepository,
    portfolioRepository: PortfolioRepository
) : ViewModel() {

    val themeMode: StateFlow<AppThemeMode> = themePreferencesManager.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppThemeMode.SYSTEM
    )

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    init {
        viewModelScope.launch {
            withTimeoutOrNull(3000L) {
                runCatching {
                    themePreferencesManager.themeMode.first()
                    val user = authRepository.getAuthState().first()
                    portfolioRepository.getPortfolio(user?.uid).first()
                }
            }
            _isReady.value = true
        }
    }
}

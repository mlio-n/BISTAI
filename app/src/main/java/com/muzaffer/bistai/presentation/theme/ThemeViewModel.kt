package com.muzaffer.bistai.presentation.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muzaffer.bistai.data.local.prefs.ThemePreferencesManager
import com.muzaffer.bistai.domain.model.AppThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    themePreferencesManager: ThemePreferencesManager
) : ViewModel() {

    val themeMode: StateFlow<AppThemeMode> = themePreferencesManager.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppThemeMode.SYSTEM
    )
}

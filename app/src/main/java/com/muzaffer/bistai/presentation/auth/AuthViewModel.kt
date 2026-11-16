package com.muzaffer.bistai.presentation.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muzaffer.bistai.domain.model.User
import com.muzaffer.bistai.domain.repository.AuthRepository
import com.muzaffer.bistai.domain.repository.PortfolioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthState(
    val user: User? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val portfolioRepository: PortfolioRepository
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState(isLoading = true))
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.getAuthState().collect { user ->
                _authState.value = AuthState(
                    user = user,
                    isLoading = false,
                    error = null
                )
            }
        }
    }

    fun signInWithGoogle(activityContext: Context) {
        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, error = null) }
            authRepository.signInWithGoogle(activityContext)
                .onSuccess { user ->
                    portfolioRepository.syncLocalToCloud(user.uid)
                    _authState.value = AuthState(
                        user = user,
                        isLoading = false,
                        error = null
                    )
                }
                .onFailure { error ->
                    _authState.update {
                        it.copy(
                            isLoading = false,
                            error = error.localizedMessage ?: "Google ile giriş yapılamadı."
                        )
                    }
                }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, error = null) }
            val result = authRepository.signInWithGoogleCredential(idToken)
            result.onSuccess { authResult ->
                authResult.user?.uid?.let { uid ->
                    portfolioRepository.syncLocalToCloud(uid)
                }
            }.onFailure { e ->
                _authState.update {
                    it.copy(
                        isLoading = false,
                        error = e.localizedMessage ?: "Giriş yapılamadı"
                    )
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    fun clearError() {
        _authState.update { it.copy(error = null) }
    }
}

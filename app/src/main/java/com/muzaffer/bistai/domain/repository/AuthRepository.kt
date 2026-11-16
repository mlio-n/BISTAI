package com.muzaffer.bistai.domain.repository

import android.content.Context
import com.google.firebase.auth.AuthResult
import com.muzaffer.bistai.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun getAuthState(): Flow<User?>
    fun getCurrentUser(): User?
    suspend fun signInWithGoogle(activityContext: Context): Result<User>
    suspend fun signInWithGoogleCredential(idToken: String): Result<AuthResult>
    suspend fun signOut()
}

package com.muzaffer.bistai.data.repository

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.muzaffer.bistai.R
import com.muzaffer.bistai.domain.model.User
import com.muzaffer.bistai.domain.repository.AuthRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val credentialManager: CredentialManager
) : AuthRepository {

    override fun getAuthState(): Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.toDomain())
        }
        auth.addAuthStateListener(listener)
        awaitClose {
            auth.removeAuthStateListener(listener)
        }
    }

    override fun getCurrentUser(): User? = auth.currentUser?.toDomain()

    override suspend fun signInWithGoogle(activityContext: Context): Result<User> {
        return try {
            val webClientId = activityContext.getString(R.string.default_web_client_id)
            val uiContext = activityContext.findActivity()

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()

            val primaryRequest = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = try {
                credentialManager.getCredential(
                    context = uiContext,
                    request = primaryRequest
                )
            } catch (_: NoCredentialException) {
                val signInOption = GetSignInWithGoogleOption.Builder(webClientId).build()
                val fallbackRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(signInOption)
                    .build()
                credentialManager.getCredential(
                    context = uiContext,
                    request = fallbackRequest
                )
            }

            val credential = response.credential
            if (credential !is CustomCredential ||
                credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                return Result.failure(IllegalStateException("Geçersiz Google kimlik bilgisi alındı."))
            }

            val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            val authResult = auth.signInWithCredential(
                GoogleAuthProvider.getCredential(idToken, null)
            ).await()

            val user = authResult.user?.toDomain()
                ?: return Result.failure(IllegalStateException("Kullanıcı oturumu açılamadı."))
            Result.success(user)
        } catch (_: GetCredentialCancellationException) {
            Result.failure(Exception("Google ile giriş iptal edildi."))
        } catch (e: GetCredentialException) {
            Result.failure(Exception(e.localizedMessage ?: "Google kimlik doğrulama başarısız."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithGoogleCredential(idToken: String): Result<AuthResult> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            Result.success(authResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        runCatching {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        }
        auth.signOut()
    }

    private fun FirebaseUser.toDomain() = User(
        uid = uid,
        email = email,
        displayName = displayName,
        photoUrl = photoUrl?.toString()
    )

    private tailrec fun Context.findActivity(): Context = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> this
    }
}

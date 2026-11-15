package com.muzaffer.bistai.presentation.auth

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.muzaffer.bistai.R
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onContinueAsGuest: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authState by authViewModel.authState.collectAsState()
    
    // Eğer hali hazırda giriş yapıldıysa doğrudan geç
    LaunchedEffect(authState.user) {
        if (authState.user != null) {
            onLoginSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // İkon / Logo
            Icon(
                imageVector = Icons.Rounded.Analytics,
                contentDescription = "BISTAI Logo",
                tint = Color(0xFF00C853),
                modifier = Modifier.size(96.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "BISTAI",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Yapay Zeka Destekli Portföy Yönetimi",
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 48.dp)
            )

            if (authState.isLoading) {
                CircularProgressIndicator(color = Color(0xFF6B21A8))
            } else {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            startGoogleSignIn(context) { idToken ->
                                if (idToken != null) {
                                    authViewModel.signInWithGoogle(idToken)
                                } else {
                                    Log.e("BISTAI_AUTH", "Google Sign in iptal edildi veya başarısız")
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    // Google Logosu Eklenecek
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google_logo_placeholder), 
                        contentDescription = "Google",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Google ile Devam Et",
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = onContinueAsGuest) {
                    Text(
                        text = "Misafir Olarak Devam Et",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                }
                
                if (authState.error != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = authState.error!!,
                        color = Color.Red,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Android 14+ Credential Manager (Play Services) API'si
 * kullanarak Google kimlik seçici dialoğunu ekrana getirir.
 */
private suspend fun startGoogleSignIn(
    context: Context,
    onResult: (String?) -> Unit
) {
    val credentialManager = CredentialManager.create(context)
    
    // Web clientId'yi values/strings.xml'den veya BuildConfig'den almalısın
    // Firebase Console -> Project Settings -> Web App OAuth Client ID
    val webClientId = context.getString(R.string.default_web_client_id)
    
    val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(webClientId)
        .setAutoSelectEnabled(true)
        .build()

    val request: GetCredentialRequest = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    try {
        val result = credentialManager.getCredential(
            request = request,
            context = context,
        )
        val credential = result.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            onResult(googleIdTokenCredential.idToken)
        } else {
            onResult(null)
        }
    } catch (e: Exception) {
        Log.e("BISTAI_AUTH", "CredentialManager Hatası", e)
        onResult(null)
    }
}

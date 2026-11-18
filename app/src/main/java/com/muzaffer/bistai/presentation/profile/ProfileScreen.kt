package com.muzaffer.bistai.presentation.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muzaffer.bistai.R
import com.muzaffer.bistai.presentation.auth.AuthViewModel
import com.muzaffer.bistai.presentation.theme.*

@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activeUser = authState.user ?: uiState.user
    var showApiKey by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(colors = listOf(PureBlack, NavyBlueMedium, PureBlack)))
    ) {
        if (activeUser == null) {
            val isAnyAuthLoading = uiState.isAuthLoading || authState.isLoading
            val combinedError = uiState.authErrorMessage ?: authState.error

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = NavyBlueSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Hesap Girişi",
                            style = MaterialTheme.typography.headlineSmall,
                            color = White,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = uiState.emailInput,
                            onValueChange = viewModel::onEmailInputChange,
                            enabled = !isAnyAuthLoading,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("E-posta", color = SlateBlue) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = SlateBlue
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BullishGreen,
                                unfocusedBorderColor = NavyBlueMedium,
                                focusedContainerColor = NavyBlueMedium,
                                unfocusedContainerColor = NavyBlueMedium,
                                cursorColor = BullishGreen,
                                focusedTextColor = White,
                                unfocusedTextColor = White
                            )
                        )

                        OutlinedTextField(
                            value = uiState.passwordInput,
                            onValueChange = viewModel::onPasswordInputChange,
                            enabled = !isAnyAuthLoading,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Şifre", color = SlateBlue) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = SlateBlue
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (showPassword) "Gizle" else "Göster",
                                        tint = SlateBlue
                                    )
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    viewModel.signInWithEmail()
                                }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BullishGreen,
                                unfocusedBorderColor = NavyBlueMedium,
                                focusedContainerColor = NavyBlueMedium,
                                unfocusedContainerColor = NavyBlueMedium,
                                cursorColor = BullishGreen,
                                focusedTextColor = White,
                                unfocusedTextColor = White
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.signInWithEmail()
                                },
                                enabled = !isAnyAuthLoading,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BullishGreen,
                                    contentColor = PureBlack
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (uiState.isAuthLoading) {
                                    CircularProgressIndicator(
                                        color = PureBlack,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Text("Giriş Yap", fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.signUpWithEmail()
                                },
                                enabled = !isAnyAuthLoading,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                border = BorderStroke(1.dp, BullishGreen),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = BullishGreen),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Kayıt Ol", fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = NavyBlueMedium)
                            Text(
                                text = "veya",
                                style = MaterialTheme.typography.labelMedium,
                                color = SlateBlue
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = NavyBlueMedium)
                        }

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                authViewModel.signInWithGoogle(context)
                            },
                            enabled = !isAnyAuthLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = White,
                                contentColor = PureBlack
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (authState.isLoading) {
                                CircularProgressIndicator(
                                    color = PureBlack,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_google_logo_placeholder),
                                    contentDescription = "Google",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Google ile Giriş Yap",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        combinedError?.let { errorMsg ->
                            Text(
                                text = errorMsg,
                                style = MaterialTheme.typography.bodySmall,
                                color = BearishRed,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BullishGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = BullishGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Profil & Ayarlar",
                            style = MaterialTheme.typography.headlineSmall,
                            color = White,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Bulut Senkronizasyonu Aktif",
                            style = MaterialTheme.typography.labelMedium,
                            color = BullishGreen
                        )
                    }
                }

                HorizontalDivider(color = NavyBlueSurface, modifier = Modifier.padding(horizontal = 16.dp))

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = NavyBlueSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = BullishGreen
                                )
                                Text(
                                    text = "Bulut Hesabı",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = White,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "Yerel Room veritabanında ${uiState.portfolioCount} portföy hissesi ve ${uiState.favoritesCount} favori hisse kayıtlı.",
                                style = MaterialTheme.typography.bodySmall,
                                color = LightSlate
                            )

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = NavyBlueMedium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = activeUser.displayName ?: activeUser.email ?: "Kullanıcı",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    activeUser.email?.let { email ->
                                        Text(
                                            text = email,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SlateBlue
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.backupToCloud() },
                                    enabled = !uiState.isSyncing,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BullishGreen,
                                        contentColor = PureBlack
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    if (uiState.isSyncing) {
                                        CircularProgressIndicator(
                                            color = PureBlack,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        Text("Buluta Yedekle", fontWeight = FontWeight.Bold)
                                    }
                                }

                                OutlinedButton(
                                    onClick = { authViewModel.signOut() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BearishRed),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Çıkış Yap", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = NavyBlueSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Key, contentDescription = null, tint = GoldAccent)
                                    Text(
                                        text = "Gemini API Anahtarı",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (uiState.isCustomApiKeySaved) BullishGreen.copy(alpha = 0.15f) else SlateBlue.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (uiState.isCustomApiKeySaved) "Şifreli Kayıtlı" else "Varsayılan",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (uiState.isCustomApiKeySaved) BullishGreen else SlateBlue,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Kendi Google AI Studio anahtarınızı girerseniz yalnızca bu cihazda EncryptedSharedPreferences ile şifreli olarak saklanır.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateBlue
                            )

                            OutlinedTextField(
                                value = uiState.apiKeyInput,
                                onValueChange = { viewModel.onApiKeyInputChange(it) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Gemini API Anahtarı", color = SlateBlue) },
                                placeholder = { Text("AIza...", color = SlateBlue.copy(alpha = 0.6f)) },
                                singleLine = true,
                                visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showApiKey = !showApiKey }) {
                                        Icon(
                                            imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (showApiKey) "Gizle" else "Göster",
                                            tint = SlateBlue
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BullishGreen,
                                    unfocusedBorderColor = NavyBlueMedium,
                                    focusedContainerColor = NavyBlueMedium,
                                    unfocusedContainerColor = NavyBlueMedium,
                                    cursorColor = BullishGreen,
                                    focusedTextColor = White,
                                    unfocusedTextColor = White
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.saveApiKey() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BullishGreen,
                                        contentColor = PureBlack
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Anahtarı Kaydet", fontWeight = FontWeight.Bold)
                                }

                                if (uiState.isCustomApiKeySaved || uiState.apiKeyInput.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = { viewModel.clearApiKey() },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LightSlate),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Temizle")
                                    }
                                }
                            }
                        }
                    }

                    uiState.statusMessage?.let { msg ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = BullishGreen.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = BullishGreen,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

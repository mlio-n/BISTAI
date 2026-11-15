package com.muzaffer.bistai.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muzaffer.bistai.presentation.auth.AuthViewModel
import com.muzaffer.bistai.presentation.theme.*

@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showApiKey by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(colors = listOf(PureBlack, NavyBlueMedium, PureBlack)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ── Başlık ───────────────────────────────────────────────────
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
                        text = if (uiState.user != null) "Bulut Senkronizasyonu Aktif" else "Misafir Modu • Yerel Depolama",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (uiState.user != null) BullishGreen else SlateBlue
                    )
                }
            }

            HorizontalDivider(color = NavyBlueSurface, modifier = Modifier.padding(horizontal = 16.dp))

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Hesap & Bulut Yedekleme Kartı ────────────────────────
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
                                imageVector = if (uiState.user != null) Icons.Default.CloudDone else Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = if (uiState.user != null) BullishGreen else GoldAccent
                            )
                            Text(
                                text = if (uiState.user != null) "Bulut Hesabı" else "Misafir Modu Aktif",
                                style = MaterialTheme.typography.titleMedium,
                                color = White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Yerel veri özeti
                        Text(
                            text = "Yerel Room veritabanında ${uiState.portfolioCount} portföy hissesi ve ${uiState.favoritesCount} favori hisse kayıtlı.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LightSlate
                        )

                        if (uiState.user == null) {
                            Text(
                                text = "Giriş yapmadan tüm özellikleri kullanabilirsiniz. Verilerinizi cihazlar arası yedeklemek isterseniz Google hesabınızla giriş yapabilirsiniz.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateBlue
                            )

                            Button(
                                onClick = onNavigateToLogin,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BullishGreen,
                                    contentColor = PureBlack
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Verilerini Buluta Yedekle (Giriş Yap)",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = NavyBlueMedium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = uiState.user?.displayName ?: "Kullanıcı",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    uiState.user?.email?.let { email ->
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
                }

                // ── BYOK (Gemini API Anahtarı) Kartı ─────────────────────
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
                                    text = "BYOK — Gemini API Key",
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

                // ── Durum Mesajı ─────────────────────────────────────────
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

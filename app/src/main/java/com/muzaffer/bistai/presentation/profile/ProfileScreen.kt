package com.muzaffer.bistai.presentation.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.muzaffer.bistai.domain.model.AppThemeMode
import com.muzaffer.bistai.presentation.auth.AuthViewModel
import com.muzaffer.bistai.presentation.theme.LocalBistaiPalette
import com.muzaffer.bistai.presentation.theme.TrendDown

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
    var showPassword by remember { mutableStateOf(false) }
    var showApiKeySheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (activeUser == null) {
            val isAnyAuthLoading = uiState.isAuthLoading || authState.isLoading
            val combinedError = uiState.authErrorMessage ?: authState.error

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = "Profil",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )

                ThemeSelectorSection(
                    selectedMode = uiState.themeMode,
                    onSelectMode = viewModel::setThemeMode
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "HESAP GİRİŞİ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = uiState.emailInput,
                        onValueChange = viewModel::onEmailInputChange,
                        enabled = !isAnyAuthLoading,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("E-posta") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Email, contentDescription = null)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = profileTextFieldColors()
                    )

                    OutlinedTextField(
                        value = uiState.passwordInput,
                        onValueChange = viewModel::onPasswordInputChange,
                        enabled = !isAnyAuthLoading,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Şifre") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showPassword) "Gizle" else "Göster"
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
                        colors = profileTextFieldColors()
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
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (uiState.isAuthLoading) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
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
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onBackground
                            ),
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
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.outline,
                        thickness = 0.5.dp
                    )
                    Text(
                        text = "veya",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.outline,
                        thickness = 0.5.dp
                    )
                    }

                    OutlinedButton(
                        onClick = {
                            focusManager.clearFocus()
                            authViewModel.signInWithGoogle(context)
                        },
                        enabled = !isAnyAuthLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (authState.isLoading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onSurface,
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
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        } else {
            val syncAgeText = remember(uiState.lastSyncedAtMillis) {
                formatRelativeSyncTime(uiState.lastSyncedAtMillis)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = "Profil",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "HESAP",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline,
                        thickness = 0.5.dp
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = activeUser.displayName?.takeIf { it.isNotBlank() } ?: activeUser.email ?: "Kullanıcı",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.SemiBold
                        )
                        activeUser.email?.takeIf { !activeUser.displayName.isNullOrBlank() && it.isNotBlank() }?.let { email ->
                            Text(
                                text = email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline,
                        thickness = 0.5.dp
                    )
                    val backupDetails = if (uiState.favoritesCount > 0) {
                        "$syncAgeText · ${uiState.portfolioCount} hisse, ${uiState.favoritesCount} favori"
                    } else {
                        "$syncAgeText · ${uiState.portfolioCount} hisse"
                    }
                    SettingsRow(
                        label = "Yedekleme",
                        value = backupDetails,
                        showChevron = false
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline,
                        thickness = 0.5.dp
                    )
                }

                ThemeSelectorSection(
                    selectedMode = uiState.themeMode,
                    onSelectMode = viewModel::setThemeMode
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "YAPAY ZEKA",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline,
                        thickness = 0.5.dp
                    )
                    SettingsRow(
                        label = "Gemini API Anahtarı",
                        value = if (uiState.isCustomApiKeySaved) "Kayıtlı" else "Ekle",
                        showChevron = true,
                        onClick = {
                            viewModel.clearStatusMessage()
                            showApiKeySheet = true
                        }
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline,
                        thickness = 0.5.dp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Çıkış Yap",
                    style = MaterialTheme.typography.titleSmall,
                    color = TrendDown,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { authViewModel.signOut() }
                        .padding(vertical = 12.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    if (showApiKeySheet) {
        ApiKeyBottomSheet(
            apiKeyInput = uiState.apiKeyInput,
            isCustomApiKeySaved = uiState.isCustomApiKeySaved,
            statusMessage = uiState.statusMessage,
            onApiKeyInputChange = viewModel::onApiKeyInputChange,
            onSave = viewModel::saveApiKey,
            onClear = viewModel::clearApiKey,
            onDismiss = { showApiKeySheet = false }
        )
    }
}

@Composable
private fun ThemeSelectorSection(
    selectedMode: AppThemeMode,
    onSelectMode: (AppThemeMode) -> Unit
) {
    val palette = LocalBistaiPalette.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "GÖRÜNÜM",
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSecondary
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = palette.surface1,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AppThemeMode.entries.forEach { mode ->
                    val isSelected = selectedMode == mode
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) palette.selectedPill else Color.Transparent,
                        shadowElevation = if (isSelected && !palette.isDark) 1.5.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelectMode(mode) }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isSelected) palette.textPrimary else palette.textSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(
    label: String,
    value: String,
    showChevron: Boolean,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            )
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Medium
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (value.isNotEmpty()) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (showChevron) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApiKeyBottomSheet(
    apiKeyInput: String,
    isCustomApiKeySaved: Boolean,
    statusMessage: String?,
    onApiKeyInputChange: (String) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showApiKey by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurfaceVariant) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Gemini API Anahtarı",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Kapat",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "Anahtarınız yalnızca bu cihazda şifreli saklanır.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = onApiKeyInputChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Gemini API Anahtarı") },
                placeholder = { Text("AIza...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                singleLine = true,
                visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showApiKey = !showApiKey }) {
                        Icon(
                            imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showApiKey) "Gizle" else "Göster"
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = profileTextFieldColors()
            )

            statusMessage?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Kaydet", fontWeight = FontWeight.Bold)
                }

                if (isCustomApiKeySaved || apiKeyInput.isNotBlank()) {
                    TextButton(
                        onClick = onClear,
                        modifier = Modifier.height(48.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = TrendDown
                        )
                    ) {
                        Text("Kaldır", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

private fun formatRelativeSyncTime(timestampMillis: Long): String {
    val diffMinutes = ((System.currentTimeMillis() - timestampMillis) / 60_000L).coerceAtLeast(0L)
    return when {
        diffMinutes < 1L -> "az önce"
        diffMinutes < 60L -> "$diffMinutes dk önce"
        else -> "${diffMinutes / 60L} saat önce"
    }
}

@Composable
private fun profileTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
    unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedTrailingIconColor = MaterialTheme.colorScheme.primary,
    unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
)

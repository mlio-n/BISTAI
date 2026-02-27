package com.muzaffer.bistai.presentation.aichat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muzaffer.bistai.presentation.components.ChatBubble
import com.muzaffer.bistai.ui.theme.*

private val QUICK_QUESTIONS = listOf(
    "BIST 100 bugün nasıl?",
    "Yeni başlayanlar için önerin?",
    "Enflasyon hisseleri etkiler mi?",
    "Temettü nedir?",
    "Dolar/TL hisseleri nasıl etkiler?",
    "Risk yönetimi nasıl yapılır?"
)

@Composable
fun AiChatScreen(viewModel: AiChatViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) listState.animateScrollToItem(uiState.messages.size - 1)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(colors = listOf(PureBlack, NavyBlueMedium, PureBlack)))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

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
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.radialGradient(colors = listOf(BullishGreen.copy(0.25f), NavyBlueMedium))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BullishGreen, modifier = Modifier.size(22.dp))
                }
                Column {
                    Text("BISTAI", style = MaterialTheme.typography.headlineSmall, color = White, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(modifier = Modifier.size(7.dp).clip(RoundedCornerShape(4.dp)).background(BullishGreen))
                        Text("Borsa Danışmanın", style = MaterialTheme.typography.labelSmall, color = BullishGreen)
                    }
                }
            }

            HorizontalDivider(color = NavyBlueSurface)

            // ── Mesaj Listesi ────────────────────────────────────────────
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.messages, key = { it.id }) { msg ->
                    ChatBubble(message = msg)
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            // ── Hazır Sorular ────────────────────────────────────────────
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(QUICK_QUESTIONS) { question ->
                    SuggestionChip(
                        onClick = {
                            viewModel.sendMessage(question)
                            focusManager.clearFocus()
                        },
                        label = { Text(question, style = MaterialTheme.typography.labelSmall, color = LightSlate) },
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = NavyBlueSurface),
                        border = SuggestionChipDefaults.suggestionChipBorder(enabled = true, borderColor = NavyBlueMedium)
                    )
                }
            }

            // ── Giriş Alanı ──────────────────────────────────────────────
            Surface(color = NavyBlueSurface, tonalElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Borsa hakkında bir şey sor...", color = SlateBlue, style = MaterialTheme.typography.bodySmall) },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BullishGreen,
                            unfocusedBorderColor = NavyBlueMedium,
                            focusedContainerColor = NavyBlueMedium,
                            unfocusedContainerColor = NavyBlueMedium,
                            cursorColor = BullishGreen,
                            focusedTextColor = White,
                            unfocusedTextColor = White
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (inputText.isNotBlank()) { viewModel.sendMessage(inputText); inputText = "" }
                        })
                    )
                    FloatingActionButton(
                        onClick = {
                            if (!uiState.isLoading && inputText.isNotBlank()) {
                                viewModel.sendMessage(inputText); inputText = ""; focusManager.clearFocus()
                            }
                        },
                        modifier = Modifier.size(48.dp),
                        containerColor = if (uiState.isLoading) SlateBlue else BullishGreen,
                        contentColor = PureBlack,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(color = White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gönder", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

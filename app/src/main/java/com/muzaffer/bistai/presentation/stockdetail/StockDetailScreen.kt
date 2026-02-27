package com.muzaffer.bistai.presentation.stockdetail

import androidx.compose.animation.*
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
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

// Hazır soru Chips listesi
private val QUICK_QUESTIONS = listOf(
    "Gelecek öngörünüz nedir?",
    "Dolar artarsa ne olur?",
    "Al/Sat için doğru zaman mı?",
    "Son çeyrek performansı?",
    "Rakipleriyle karşılaştır"
)

@Composable
fun StockDetailScreen(
    symbol: String,
    onBack: () -> Unit,
    viewModel: StockDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    // Yeni mesaj gelince otomatik scroll
    LaunchedEffect(uiState.chatMessages.size) {
        if (uiState.chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.chatMessages.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(colors = listOf(PureBlack, NavyBlueMedium, PureBlack)))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top Bar ────────────────────────────────────────────────────
            TopBar(symbol = symbol, onBack = onBack, onRefresh = { viewModel.fetchAnalysis(symbol) })

            // ── Scrollable Content ─────────────────────────────────────────
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Sembol rozeti
                item { SymbolBadge(symbol = symbol) }

                // Gemini tek seferlik analiz kartı
                item {
                    AnimatedContent(
                        targetState = Triple(uiState.isLoading, uiState.errorMessage, uiState.analysis),
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "ai_content"
                    ) { (loading, error, analysis) ->
                        when {
                            loading          -> AiLoadingCard()
                            error != null    -> AiErrorCard(message = error, onRetry = { viewModel.fetchAnalysis(symbol) })
                            analysis != null -> AiAnalysisCard(text = analysis)
                            else             -> AiLoadingCard()
                        }
                    }
                }

                // Chat bölümü başlığı
                if (uiState.chatMessages.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(color = NavyBlueSurface)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BullishGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) { Text("✦", color = BullishGreen, fontSize = 12.sp) }
                            Text(
                                "AI Danışman Sohbeti",
                                style = MaterialTheme.typography.titleSmall,
                                color = LightSlate,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                // Mesaj balonları
                items(uiState.chatMessages, key = { it.id }) { msg ->
                    ChatBubble(message = msg)
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            // ── Hazır Sorular (Chips) ──────────────────────────────────────
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(QUICK_QUESTIONS) { question ->
                    SuggestionChip(
                        onClick = {
                            viewModel.sendChatMessage(question)
                            focusManager.clearFocus()
                        },
                        label = {
                            Text(
                                question,
                                style = MaterialTheme.typography.labelSmall,
                                color = LightSlate
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = NavyBlueSurface
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = NavyBlueMedium
                        )
                    )
                }
            }

            // ── Mesaj Giriş Alanı ─────────────────────────────────────────
            ChatInputBar(
                symbol = symbol,
                value = inputText,
                onValueChange = { inputText = it },
                isSending = uiState.isChatLoading,
                onSend = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendChatMessage(inputText)
                        inputText = ""
                        focusManager.clearFocus()
                    }
                }
            )
        }
    }
}

// ─── Chat Input Bar ──────────────────────────────────────────────────────────

@Composable
private fun ChatInputBar(
    symbol: String,
    value: String,
    onValueChange: (String) -> Unit,
    isSending: Boolean,
    onSend: () -> Unit
) {
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
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text("$symbol hakkında bir şey sor...", color = SlateBlue, style = MaterialTheme.typography.bodySmall)
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = BullishGreen,
                    unfocusedBorderColor    = NavyBlueMedium,
                    focusedContainerColor   = NavyBlueMedium,
                    unfocusedContainerColor = NavyBlueMedium,
                    cursorColor             = BullishGreen,
                    focusedTextColor        = White,
                    unfocusedTextColor      = White
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() })
            )
            // Gönder butonu
            FloatingActionButton(
                onClick = { if (!isSending) onSend() },
                modifier = Modifier.size(48.dp),
                containerColor = if (isSending) SlateBlue else BullishGreen,
                contentColor = PureBlack,
                elevation = FloatingActionButtonDefaults.elevation(0.dp)
            ) {
                if (isSending) {
                    CircularProgressIndicator(color = White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gönder", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// ─── Top Bar ─────────────────────────────────────────────────────────────────

@Composable
private fun TopBar(symbol: String, onBack: () -> Unit, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = White)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(symbol, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = White, letterSpacing = 1.sp)
                Text("Hisse Detayı & AI Danışman", style = MaterialTheme.typography.labelMedium, color = SlateBlue)
            }
        }
        IconButton(onClick = onRefresh, colors = IconButtonDefaults.iconButtonColors(containerColor = NavyBlueSurface, contentColor = SlateBlue)) {
            Icon(Icons.Default.Refresh, contentDescription = "Analizi Yenile")
        }
    }
}

// ─── Symbol Badge ─────────────────────────────────────────────────────────────

@Composable
private fun SymbolBadge(symbol: String) {
    Surface(shape = RoundedCornerShape(20.dp), color = NavyBlueSurface, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.radialGradient(colors = listOf(BullishGreen.copy(alpha = 0.2f), NavyBlueMedium))),
                contentAlignment = Alignment.Center
            ) {
                Text(symbol.take(2), style = MaterialTheme.typography.titleLarge, color = BullishGreen, fontWeight = FontWeight.Black)
            }
            Column {
                Text(symbol, style = MaterialTheme.typography.titleLarge, color = White, fontWeight = FontWeight.Bold)
                Text("BIST • Türk Lirası", style = MaterialTheme.typography.bodySmall, color = SlateBlue)
            }
        }
    }
}

// ─── Analysis Cards ──────────────────────────────────────────────────────────

@Composable
fun AiAnalysisCard(text: String) {
    Surface(shape = RoundedCornerShape(20.dp), color = NavyBlueSurface, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(BullishGreen.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Text("✦", color = BullishGreen, style = MaterialTheme.typography.bodyMedium)
                }
                Column {
                    Text("Gemini AI Analizi", style = MaterialTheme.typography.titleSmall, color = White, fontWeight = FontWeight.Bold)
                    Text("Google Gemini 1.5 Flash", style = MaterialTheme.typography.labelSmall, color = SlateBlue)
                }
            }
            HorizontalDivider(color = NavyBlueMedium)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = LightSlate, lineHeight = 22.sp)
            Surface(shape = RoundedCornerShape(10.dp), color = GoldAccent.copy(alpha = 0.08f)) {
                Text("⚠️  Bu analiz bilgi amaçlıdır, yatırım tavsiyesi değildir.", style = MaterialTheme.typography.labelSmall, color = GoldAccent, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
            }
        }
    }
}

@Composable
fun AiLoadingCard() {
    Surface(shape = RoundedCornerShape(20.dp), color = NavyBlueSurface, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            CircularProgressIndicator(color = BullishGreen, strokeWidth = 2.dp, modifier = Modifier.size(36.dp))
            Text("Gemini analiz üretiyor...", style = MaterialTheme.typography.bodyMedium, color = SlateBlue)
            Text("5-10 saniye sürebilir", style = MaterialTheme.typography.labelSmall, color = SlateBlue.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun AiErrorCard(message: String, onRetry: () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = NavyBlueSurface, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(36.dp))
            Text("Analiz Yapılamadı", style = MaterialTheme.typography.titleMedium, color = White, fontWeight = FontWeight.Bold)
            Text(message, style = MaterialTheme.typography.bodySmall, color = SlateBlue)
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = NavyBlueMedium, contentColor = LightSlate), shape = RoundedCornerShape(12.dp)) {
                Text("Tekrar Dene", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

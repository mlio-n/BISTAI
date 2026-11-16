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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
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
import com.muzaffer.bistai.domain.model.StockQuickAction
import com.muzaffer.bistai.domain.repository.AnalysisSource
import com.muzaffer.bistai.presentation.components.AiQuickActionBottomSheet
import com.muzaffer.bistai.presentation.components.ChatBubble
import com.muzaffer.bistai.presentation.theme.*
import kotlinx.coroutines.launch

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
    var showPortfolioSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
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

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(StockQuickAction.entries) { action ->
                    val isSelected = uiState.selectedQuickAction == action && uiState.isQuickActionSheetVisible
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.executeQuickAction(action) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            Text(
                                text = action.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = NavyBlueSurface,
                            labelColor = LightSlate,
                            iconColor = BullishGreen,
                            selectedContainerColor = BullishGreen.copy(alpha = 0.18f),
                            selectedLabelColor = BullishGreen,
                            selectedLeadingIconColor = BullishGreen
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = NavyBlueMedium,
                            selectedBorderColor = BullishGreen.copy(alpha = 0.6f)
                        )
                    )
                }
            }

            // ── Scrollable Content ─────────────────────────────────────────
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // AI Tahmin Kartı — listenin en üstünde
                item {
                    uiState.prediction?.let { pred ->
                        AiPredictionCard(
                            prediction   = pred,
                            source       = uiState.analysisSource,
                            ageMinutes   = uiState.analysisAgeMinutes
                        )
                    }
                }

                // Sembol rozeti
                item {
                    SymbolBadge(
                        symbol = symbol,
                        onAddToPortfolioClick = { showPortfolioSheet = true }
                    )
                }

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
                            ) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = BullishGreen, modifier = Modifier.size(14.dp))
                            }
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

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp)
        )
    }

    if (showPortfolioSheet) {
        PortfolioBottomSheet(
            symbol = symbol,
            onDismiss = { showPortfolioSheet = false },
            onSave = { lot, cost ->
                viewModel.addToPortfolio(lot, cost) { _, msg ->
                    showPortfolioSheet = false
                    if (!msg.isNullOrBlank()) {
                        coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                    }
                }
            }
        )
    }

    if (uiState.isQuickActionSheetVisible) {
        AiQuickActionBottomSheet(
            title = uiState.selectedQuickAction?.label ?: "Hızlı Analiz",
            subtitle = "$symbol • BISTAI Analitik Rapor",
            isLoading = uiState.isQuickActionLoading,
            markdownResult = uiState.quickActionResult,
            errorMessage = uiState.quickActionError,
            onRetry = {
                uiState.selectedQuickAction?.let { viewModel.executeQuickAction(it) }
            },
            onDismiss = { viewModel.dismissQuickActionSheet() }
        )
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
private fun SymbolBadge(
    symbol: String,
    onAddToPortfolioClick: () -> Unit
) {
    Surface(shape = RoundedCornerShape(20.dp), color = NavyBlueSurface, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
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
            FilledTonalButton(
                onClick = onAddToPortfolioClick,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = BullishGreen.copy(alpha = 0.15f),
                    contentColor = BullishGreen
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Portföye Ekle", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─── AI Prediction Card ─────────────────────────────────────────────────────

@Composable
fun AiPredictionCard(
    prediction: com.muzaffer.bistai.domain.model.AiPrediction,
    source: AnalysisSource? = null,
    ageMinutes: Long? = null
) {
    val trend = prediction.trend
    val trendColor = when (trend) {
        com.muzaffer.bistai.domain.model.Trend.BULLISH -> BullishGreen
        com.muzaffer.bistai.domain.model.Trend.BEARISH -> BearishRed
        com.muzaffer.bistai.domain.model.Trend.NEUTRAL -> GoldAccent
    }
    // Kaynak badge bilgisi
    val (sourceEmoji, sourceLabel, sourceColor) = when (source) {
        AnalysisSource.FIREBASE    -> Triple("🔥", "Firebase", BullishGreen)
        AnalysisSource.LOCAL_CACHE -> Triple("💾", "Yerel önbellek", GoldAccent)
        AnalysisSource.FRESH_API   -> Triple("✨", "Taze analiz", SlateBlue)
        null                       -> Triple("", "", SlateBlue)
    }
    val ageText = ageMinutes?.let {
        when {
            it < 60   -> "$it dk önce"
            it < 1440 -> "${it / 60} saat önce"
            else      -> "${it / 1440} gün önce"
        }
    } ?: ""

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = NavyBlueSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // ── Başlık ──────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                    Text("BISTAI Tahmini", style = MaterialTheme.typography.titleSmall, color = White, fontWeight = FontWeight.Bold)
                }
                Surface(shape = RoundedCornerShape(8.dp), color = trendColor.copy(alpha = 0.15f)) {
                    Text(
                        "${trend.emoji} ${trend.label}",
                        color = trendColor, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            // ── Kaynak Badge ─────────────────────────────────────────────
            if (source != null) {
                Surface(shape = RoundedCornerShape(8.dp), color = sourceColor.copy(alpha = 0.08f)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(sourceEmoji, style = MaterialTheme.typography.labelSmall)
                        Text(
                            if (ageText.isNotEmpty()) "$sourceLabel • $ageText" else sourceLabel,
                            style = MaterialTheme.typography.labelSmall, color = sourceColor
                        )
                    }
                }
            }
            HorizontalDivider(color = NavyBlueMedium)
            // ── Güven Skoru ─────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Yapay Zeka Güven Skoru", style = MaterialTheme.typography.labelSmall, color = SlateBlue)
                    Text("%${prediction.confidenceScore}", style = MaterialTheme.typography.labelSmall, color = trendColor, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(
                    progress = { prediction.confidenceScore / 100f },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).height(6.dp),
                    color = trendColor,
                    trackColor = NavyBlueMedium
                )
            }
            // ── Hedef Fiyat ──────────────────────────────────────────────
            if (prediction.targetLow > 0 || prediction.targetHigh > 0) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Tahmini Hedef Aralık", style = MaterialTheme.typography.labelSmall, color = SlateBlue)
                    Text(
                        "${prediction.targetLow} — ${prediction.targetHigh}",
                        style = MaterialTheme.typography.labelMedium, color = White, fontWeight = FontWeight.SemiBold
                    )
                }
            }
            // ── Gerekçe ──────────────────────────────────────────────────
            Surface(shape = RoundedCornerShape(10.dp), color = trendColor.copy(alpha = 0.06f)) {
                Text(
                    prediction.reasoning,
                    style = MaterialTheme.typography.bodySmall, color = LightSlate, lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun AiAnalysisCard(text: String) {
    Surface(shape = RoundedCornerShape(20.dp), color = NavyBlueSurface, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(BullishGreen.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = BullishGreen, modifier = Modifier.size(18.dp))
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

// ─── Portfolio Bottom Sheet ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioBottomSheet(
    symbol: String,
    onDismiss: () -> Unit,
    onSave: (Double, Double) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var lotSize by remember { mutableStateOf("") }
    var averageCost by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NavyBlueSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = SlateBlue) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Portföye Ekle: $symbol",
                style = MaterialTheme.typography.titleLarge,
                color = White,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = lotSize,
                onValueChange = { lotSize = it },
                label = { Text("Lot (Adet)", color = SlateBlue) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BullishGreen,
                    unfocusedBorderColor = NavyBlueMedium,
                    focusedTextColor = White,
                    unfocusedTextColor = White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = averageCost,
                onValueChange = { averageCost = it },
                label = { Text("Alış Maliyeti (₺)", color = SlateBlue) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BullishGreen,
                    unfocusedBorderColor = NavyBlueMedium,
                    focusedTextColor = White,
                    unfocusedTextColor = White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val lot = lotSize.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val cost = averageCost.replace(",", ".").toDoubleOrNull() ?: 0.0
                    if (lot > 0 && cost >= 0) {
                        onSave(lot, cost)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BullishGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Portföye Kaydet",
                    color = PureBlack,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

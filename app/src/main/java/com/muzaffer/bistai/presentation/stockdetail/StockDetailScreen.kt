package com.muzaffer.bistai.presentation.stockdetail

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muzaffer.bistai.ui.theme.*

@Composable
fun StockDetailScreen(
    symbol: String,
    onBack: () -> Unit,
    viewModel: StockDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(PureBlack, NavyBlueMedium, PureBlack)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ── Top Bar ────────────────────────────────────────────────────
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
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                            tint = White
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = symbol,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = White,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Hisse Detayı",
                            style = MaterialTheme.typography.labelMedium,
                            color = SlateBlue
                        )
                    }
                }
                // Yenile butonu
                IconButton(
                    onClick = { viewModel.fetchAnalysis(symbol) },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = NavyBlueSurface,
                        contentColor = SlateBlue
                    )
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Yenile")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Sembol Rozeti ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = NavyBlueSurface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Sembol ikonu
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            BullishGreen.copy(alpha = 0.2f),
                                            NavyBlueMedium
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = symbol.take(2),
                                style = MaterialTheme.typography.titleLarge,
                                color = BullishGreen,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Column {
                            Text(
                                text = symbol,
                                style = MaterialTheme.typography.titleLarge,
                                color = White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "BIST • Türk Lirası",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateBlue
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Yapay Zekâ Analiz Kutusu ──────────────────────────────────
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                AnimatedContent(
                    targetState = Triple(uiState.isLoading, uiState.errorMessage, uiState.analysis),
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "ai_content"
                ) { (loading, error, analysis) ->
                    when {
                        loading  -> AiLoadingCard()
                        error != null -> AiErrorCard(
                            message = error,
                            onRetry = { viewModel.fetchAnalysis(symbol) }
                        )
                        analysis != null -> AiAnalysisCard(text = analysis)
                        else -> AiLoadingCard() // başlangıç
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ─── AI Analiz Kartı ──────────────────────────────────────────────────────────

@Composable
private fun AiAnalysisCard(text: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = NavyBlueSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Başlık
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(BullishGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✦", color = BullishGreen, style = MaterialTheme.typography.titleMedium)
                }
                Column {
                    Text(
                        text = "Gemini AI Analizi",
                        style = MaterialTheme.typography.titleSmall,
                        color = White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Google Gemini 1.5 Flash tarafından üretildi",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateBlue
                    )
                }
            }

            Divider(color = NavyBlueMedium, thickness = 1.dp)

            // Analiz metni — emoji başlıkları renklendirilerek gösterilir
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = LightSlate,
                lineHeight = 22.sp
            )

            // Uyarı notu
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = GoldAccent.copy(alpha = 0.08f)
            ) {
                Text(
                    text = "⚠️  Bu analiz bilgi amaçlıdır, yatırım tavsiyesi değildir.",
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldAccent,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

// ─── Yükleniyor Kartı ────────────────────────────────────────────────────────

@Composable
private fun AiLoadingCard() {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = NavyBlueSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = BullishGreen,
                strokeWidth = 2.dp,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = "Gemini analiz üretiyor...",
                style = MaterialTheme.typography.bodyMedium,
                color = SlateBlue
            )
            Text(
                text = "Bu işlem 5-10 saniye sürebilir",
                style = MaterialTheme.typography.labelSmall,
                color = SlateBlue.copy(alpha = 0.6f)
            )
        }
    }
}

// ─── Hata Kartı ──────────────────────────────────────────────────────────────

@Composable
private fun AiErrorCard(message: String, onRetry: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = NavyBlueSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = GoldAccent,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = "Analiz Yapılamadı",
                style = MaterialTheme.typography.titleMedium,
                color = White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = SlateBlue
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NavyBlueMedium,
                    contentColor = LightSlate
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Tekrar Dene", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

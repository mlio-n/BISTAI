package com.muzaffer.bistai.presentation.portfolio

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muzaffer.bistai.domain.model.Stock
import com.muzaffer.bistai.ui.theme.*

@Composable
fun PortfolioScreen(
    viewModel: PortfolioViewModel = hiltViewModel()
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
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top App Bar ───────────────────────────────────────────────
            PortfolioTopBar(onRefreshClick = { viewModel.refresh() })

            // ── Content ───────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> LoadingState()                        // Yükleniyor
                    uiState.hasError  -> ErrorState(
                        message   = uiState.errorMessage ?: "Bilinmeyen hata",
                        onRetry   = { viewModel.refresh() }
                    )                                                          // Hata
                    uiState.isEmpty   -> EmptyState()                          // Boş
                    else              -> StockList(stocks = uiState.stocks)    // Başarılı
                }
            }
        }
    }
}

// ─── Top App Bar ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PortfolioTopBar(onRefreshClick: () -> Unit) {
    Surface(color = Color.Transparent, shadowElevation = 0.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "BISTAI",
                    style = MaterialTheme.typography.headlineMedium,
                    color = White,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(BullishGreen)
                    )
                    Text(
                        text = "CANLI",
                        style = MaterialTheme.typography.labelSmall,
                        color = BullishGreen,
                        letterSpacing = 1.5.sp
                    )
                }
            }
            IconButton(
                onClick = onRefreshClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = NavyBlueSurface,
                    contentColor = LightSlate
                )
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Yenile")
            }
        }
    }
}

// ─── Stock List ───────────────────────────────────────────────────────────────

@Composable
private fun StockList(stocks: List<Stock>) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "Portföy  •  ${stocks.size} Hisse",
                style = MaterialTheme.typography.labelMedium,
                color = SlateBlue,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }
        items(stocks, key = { it.symbol }) { stock ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically()
            ) {
                StockCard(stock = stock)
            }
        }
    }
}

// ─── Stock Card ───────────────────────────────────────────────────────────────

@Composable
fun StockCard(stock: Stock) {
    val changeColor = if (stock.isBullish) BullishGreen else BearishRed
    val changeBg    = if (stock.isBullish) BullishGreen.copy(alpha = 0.12f)
                     else BearishRed.copy(alpha = 0.12f)

    Surface(
        shape  = RoundedCornerShape(18.dp),
        color  = NavyBlueSurface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Sol: Sembol & İsim
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = stock.symbol,
                    style = MaterialTheme.typography.titleMedium,
                    color = White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text     = stock.name,
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = SlateBlue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // ── Sağ: Fiyat & Değişim
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text  = "₺%.2f".format(stock.currentPrice),
                    style = MaterialTheme.typography.titleMedium,
                    color = White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(changeBg)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text  = stock.formattedChangePercent,
                        style = MaterialTheme.typography.labelMedium,
                        color = changeColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ─── Loading State ────────────────────────────────────────────────────────────

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = BullishGreen,
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text  = "Piyasalar yükleniyor...",
                style = MaterialTheme.typography.bodyMedium,
                color = SlateBlue
            )
        }
    }
}

// ─── Error State ──────────────────────────────────────────────────────────────

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = BearishRed,
                modifier = Modifier.size(52.dp)
            )
            Text(
                text  = "Bağlantı Hatası",
                style = MaterialTheme.typography.titleLarge,
                color = White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text  = message,
                style = MaterialTheme.typography.bodyMedium,
                color = SlateBlue
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BullishGreen,
                    contentColor   = PureBlack
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Tekrar Dene", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─── Empty State ──────────────────────────────────────────────────────────────

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text  = "Henüz hisse verisi yok",
            style = MaterialTheme.typography.bodyLarge,
            color = SlateBlue
        )
    }
}

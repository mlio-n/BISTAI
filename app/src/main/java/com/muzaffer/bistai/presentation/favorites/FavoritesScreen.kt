package com.muzaffer.bistai.presentation.favorites

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
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
import com.muzaffer.bistai.presentation.portfolio.PortfolioViewModel
import com.muzaffer.bistai.presentation.portfolio.StockCard
import com.muzaffer.bistai.ui.theme.*

@Composable
fun FavoritesScreen(
    onStockClick: (String) -> Unit = {},
    viewModel: PortfolioViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val favoriteStocks = remember(uiState.stocks, uiState.favoriteSymbols) {
        uiState.stocks.filter { it.symbol in uiState.favoriteSymbols }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(colors = listOf(PureBlack, NavyBlueMedium, PureBlack)))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Başlık ──────────────────────────────────────────────────
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
                        .background(BearishRed.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = BearishRed, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text("Favorilerim", style = MaterialTheme.typography.headlineSmall, color = White, fontWeight = FontWeight.Black)
                    Text("${favoriteStocks.size} hisse takip ediliyor", style = MaterialTheme.typography.labelMedium, color = SlateBlue)
                }
            }

            HorizontalDivider(color = NavyBlueSurface, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(8.dp))

            if (favoriteStocks.isEmpty()) {
                // Boş durum
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text("❤️", fontSize = 52.sp)
                        Text("Henüz favori eklenmedi", style = MaterialTheme.typography.titleMedium, color = White, fontWeight = FontWeight.Bold)
                        Text(
                            "Portföy ekranında hisse kartlarındaki kalp ikonuna basarak favorine ekleyebilirsin.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SlateBlue
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(favoriteStocks, key = { it.symbol }) { stock ->
                        AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically()) {
                            StockCard(
                                stock = stock,
                                isFavorite = true,
                                onClick = { onStockClick(stock.symbol) },
                                onFavoriteClick = { viewModel.toggleFavorite(stock.symbol, stock.name) }
                            )
                        }
                    }
                }
            }
        }
    }
}

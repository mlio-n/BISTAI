package com.muzaffer.bistai.presentation.portfolio

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import com.muzaffer.bistai.presentation.components.ShimmerStockCard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muzaffer.bistai.domain.model.Stock
import com.muzaffer.bistai.ui.theme.*

@Composable
fun PortfolioScreen(
    onStockClick: (String) -> Unit = {},
    viewModel: PortfolioViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }

    // Arama filtresi
    val filteredStocks = remember(uiState.stocks, searchQuery) {
        if (searchQuery.isBlank()) uiState.stocks
        else uiState.stocks.filter {
            it.symbol.contains(searchQuery, ignoreCase = true) ||
            it.name.contains(searchQuery, ignoreCase = true)
        }
    }

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
            PortfolioTopBar(onRefreshClick = { viewModel.refresh() })

            // ── Arama Çubuğu ─────────────────────────────────────────────
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // ── İçerik ───────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> LoadingState()
                    uiState.hasError  -> ErrorState(
                        message = uiState.errorMessage ?: "Bilinmeyen hata",
                        onRetry = { viewModel.refresh() }
                    )
                    uiState.isEmpty   -> EmptyState()
                    else              -> StockList(
                        stocks = filteredStocks,
                        searchQuery = searchQuery,
                        onStockClick = onStockClick
                    )
                }
            }
        }
    }
}

// ─── Search Bar ──────────────────────────────────────────────────────────────

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = "Hisse ara... (THYAO, Garanti...)",
                color = SlateBlue,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Ara",
                tint = SlateBlue
            )
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                TextButton(onClick = { onQueryChange("") }) {
                    Text("✕", color = SlateBlue)
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = BullishGreen,
            unfocusedBorderColor = NavyBlueSurface,
            focusedContainerColor   = NavyBlueSurface,
            unfocusedContainerColor = NavyBlueSurface,
            cursorColor          = BullishGreen,
            focusedTextColor     = White,
            unfocusedTextColor   = White
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
    )
}

// ─── Top App Bar ─────────────────────────────────────────────────────────────

@Composable
private fun PortfolioTopBar(onRefreshClick: () -> Unit) {
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

// ─── Stock List ───────────────────────────────────────────────────────────────

@Composable
private fun StockList(
    stocks: List<Stock>,
    searchQuery: String,
    onStockClick: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            val label = if (searchQuery.isBlank())
                "Portföy  •  ${stocks.size} Hisse"
            else
                "\"$searchQuery\" için ${stocks.size} sonuç"
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = SlateBlue,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }

        if (stocks.isEmpty() && searchQuery.isNotBlank()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\"$searchQuery\" bulunamadı",
                        color = SlateBlue,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            items(stocks, key = { it.symbol }) { stock ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically()
                ) {
                    StockCard(
                        stock = stock,
                        onClick = { onStockClick(stock.symbol) }
                    )
                }
            }
        }
    }
}

// ─── Stock Card ───────────────────────────────────────────────────────────────

@Composable
fun StockCard(stock: Stock, onClick: () -> Unit = {}) {
    val changeColor = if (stock.isBullish) BullishGreen else BearishRed
    val changeBg    = if (stock.isBullish) BullishGreen.copy(alpha = 0.12f)
                      else BearishRed.copy(alpha = 0.12f)

    Surface(
        shape  = RoundedCornerShape(18.dp),
        color  = NavyBlueSurface,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sol: Sembol & İsim
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

            // Sağ: Fiyat & Değişim
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

// ─── States ───────────────────────────────────────────────────────────────────

@Composable
private fun LoadingState() {
    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        userScrollEnabled = false
    ) {
        // Başlık skeleton
        item {
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(NavyBlueSurface)
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }
        // 7 adet shimmer kart
        items(7) {
            ShimmerStockCard()
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = BearishRed, modifier = Modifier.size(52.dp))
            Text("Bağlantı Hatası", style = MaterialTheme.typography.titleLarge, color = White, fontWeight = FontWeight.Bold)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = SlateBlue)
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = BullishGreen, contentColor = PureBlack),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Tekrar Dene", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Henüz hisse verisi yok", style = MaterialTheme.typography.bodyLarge, color = SlateBlue)
    }
}

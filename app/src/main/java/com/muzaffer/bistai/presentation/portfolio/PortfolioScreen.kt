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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
import com.muzaffer.bistai.presentation.components.ShimmerStockCard
import com.muzaffer.bistai.presentation.theme.*

@Composable
fun PortfolioScreen(
    onStockClick: (String) -> Unit = {},
    viewModel: PortfolioViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }

    // Arama filtresi
    val filteredStocks = remember(uiState.stocks, searchQuery) {
        var list = uiState.stocks
        if (searchQuery.isNotBlank()) list = list.filter {
            it.symbol.contains(searchQuery, ignoreCase = true) ||
            it.name.contains(searchQuery, ignoreCase = true)
        }
        list
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(colors = listOf(PureBlack, NavyBlueMedium, PureBlack)))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PortfolioTopBar(onRefreshClick = { viewModel.refresh() })

            // ── Arama ────────────────────────────────────────────────
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)
            )

            // ── İçerik ──────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> LoadingState()
                    uiState.hasError  -> ErrorState(
                        message = uiState.errorMessage ?: "Bilinmeyen hata",
                        onRetry = { viewModel.refresh() }
                    )
                    uiState.isEmpty   -> EmptyState()
                    else              -> StockList(
                        uiState          = uiState, // PortfolioUiState'in tamamını gönderiyoruz
                        searchQuery      = searchQuery,
                        filteredStocks   = filteredStocks, // Arama sonuçları
                        onStockClick     = onStockClick,
                        onFavoriteToggle = { symbol, name ->
                            viewModel.toggleFavorite(symbol, name)
                        }
                    )
                }
            }
        }
    }
}

// ─── Search Bar ──────────────────────────────────────────────────────────────

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = {
            Text("Hisse ara...", color = SlateBlue, style = MaterialTheme.typography.bodyMedium)
        },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Ara", tint = SlateBlue)
        },
        trailingIcon = {
            if (query.isNotBlank()) TextButton(onClick = { onQueryChange("") }) { Text("✕", color = SlateBlue) }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = BullishGreen,
            unfocusedBorderColor    = NavyBlueSurface,
            focusedContainerColor   = NavyBlueSurface,
            unfocusedContainerColor = NavyBlueSurface,
            cursorColor             = BullishGreen,
            focusedTextColor        = White,
            unfocusedTextColor      = White
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
            Text("BISTAI", style = MaterialTheme.typography.headlineMedium, color = White, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(BullishGreen))
                Text("CANLI", style = MaterialTheme.typography.labelSmall, color = BullishGreen, letterSpacing = 1.5.sp)
            }
        }
        IconButton(
            onClick = onRefreshClick,
            colors = IconButtonDefaults.iconButtonColors(containerColor = NavyBlueSurface, contentColor = LightSlate)
        ) { Icon(Icons.Default.Refresh, contentDescription = "Yenile") }
    }
}

// ─── Stock List & Dashboard ───────────────────────────────────────────────────

@Composable
private fun StockList(
    uiState: PortfolioUiState,
    searchQuery: String,
    filteredStocks: List<Stock>,
    onStockClick: (String) -> Unit,
    onFavoriteToggle: (String, String) -> Unit
) {
    val stocks = filteredStocks
    
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Eğer arama yapılmıyorsa ve kullanıcı giriş yaptıysa cüzdan özetini en üstte göster
        if (searchQuery.isBlank() && uiState.isAuthenticated) {
            item {
                PortfolioSummaryCard(
                    totalValue = uiState.currentTotalValue,
                    totalInvestment = uiState.totalInvestment,
                    profitLoss = uiState.totalProfitLoss,
                    profitLossPercentage = uiState.totalProfitLossPercentage
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        // Portföydeki Hisse Sayısı veya Arama Sonucu
        item {
            val label = if (searchQuery.isNotBlank())
                "\"$searchQuery\" için ${stocks.size} sonuç"
            else
                "Tüm Piyasalar  •  ${stocks.size} Hisse"
            Text(label, style = MaterialTheme.typography.labelMedium, color = SlateBlue,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
        }

        if (stocks.isEmpty() && searchQuery.isNotBlank()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                    Text("\"$searchQuery\" bulunamadı", color = SlateBlue, style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            items(stocks, key = { it.symbol }) { stock ->
                // Bu hisse cüzdanda var mı kontrolü
                val portfolioItem = uiState.portfolioItems.find { it.symbol == stock.symbol }
                
                AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically()) {
                    StockCard(
                        stock = stock,
                        isFavorite = stock.symbol in uiState.favoriteSymbols,
                        portfolioItem = portfolioItem, // Lot miktarını Badge içinde göstermek için
                        onClick = { onStockClick(stock.symbol) },
                        onFavoriteClick = { onFavoriteToggle(stock.symbol, stock.name) }
                    )
                }
            }
        }
    }
}

// ─── Portfolio Summary Card (Cüzdan Özeti) ──────────────────────────────────

@Composable
fun PortfolioSummaryCard(
    totalValue: Double,
    totalInvestment: Double,
    profitLoss: Double,
    profitLossPercentage: Double
) {
    val isProfit = profitLoss >= 0
    val changeColor = if (isProfit) BullishGreen else BearishRed
    val sign = if (isProfit) "+" else ""

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = NavyBlueSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Cüzdan Özeti", style = MaterialTheme.typography.labelMedium, color = SlateBlue)
            
            Column {
                Text(
                    "₺%.2f".format(totalValue),
                    style = MaterialTheme.typography.headlineLarge,
                    color = White,
                    fontWeight = FontWeight.Black
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "$sign₺%.2f".format(profitLoss),
                        style = MaterialTheme.typography.titleMedium,
                        color = changeColor,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(shape = RoundedCornerShape(6.dp), color = changeColor.copy(alpha = 0.15f)) {
                        Text(
                            "$sign%.2f%%".format(profitLossPercentage),
                            style = MaterialTheme.typography.labelSmall,
                            color = changeColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            
            HorizontalDivider(color = NavyBlueMedium)
            
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Yatırılan", style = MaterialTheme.typography.labelSmall, color = SlateBlue)
                    Text("₺%.2f".format(totalInvestment), style = MaterialTheme.typography.labelLarge, color = LightSlate)
                }
            }
        }
    }
}

// ─── Stock Card (Sahip Olunan Lot Badge'li) ────────────────────────────────────────────────

@Composable
fun StockCard(
    stock: Stock,
    isFavorite: Boolean = false,
    portfolioItem: com.muzaffer.bistai.domain.model.PortfolioItem? = null,
    onClick: () -> Unit = {},
    onFavoriteClick: () -> Unit = {}
) {
    val changeColor = if (stock.isBullish) BullishGreen else BearishRed
    val changeBg    = if (stock.isBullish) BullishGreen.copy(alpha = 0.12f) else BearishRed.copy(alpha = 0.12f)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = NavyBlueSurface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 6.dp, top = 14.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sol: Sembol & İsim & Varsa Lot Sayısı
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stock.symbol, style = MaterialTheme.typography.titleMedium, color = White, fontWeight = FontWeight.Bold)
                    
                    if (portfolioItem != null && portfolioItem.lotSize > 0) {
                        Surface(shape = RoundedCornerShape(4.dp), color = SlateBlue.copy(alpha = 0.2f)) {
                            Text(
                                "${portfolioItem.lotSize} Lot", 
                                style = MaterialTheme.typography.labelSmall, 
                                color = LightSlate, 
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(stock.name, style = MaterialTheme.typography.bodyMedium, color = SlateBlue, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            // Sağ: Fiyat + Badge + Kalp
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("₺%.2f".format(stock.currentPrice), style = MaterialTheme.typography.titleMedium, color = White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(changeBg).padding(horizontal = 10.dp, vertical = 3.dp)) {
                        Text(stock.formattedChangePercent, style = MaterialTheme.typography.labelMedium, color = changeColor, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Kalp butonu
                IconButton(onClick = onFavoriteClick) {
                    AnimatedContent(targetState = isFavorite, label = "heart") { fav ->
                        Icon(
                            imageVector = if (fav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (fav) "Favoriden çıkar" else "Favoriye ekle",
                            tint = if (fav) BearishRed else SlateBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── States ───────────────────────────────────────────────────────────────────

@Composable
private fun LoadingState() {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        userScrollEnabled = false
    ) {
        item {
            Box(modifier = Modifier.width(160.dp).height(14.dp).clip(RoundedCornerShape(6.dp)).background(NavyBlueSurface))
        }
        items(7) { ShimmerStockCard() }
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
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = BullishGreen, contentColor = PureBlack), shape = RoundedCornerShape(12.dp)) {
                Text("Tekrar Dene", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Henüz hisse verisi yok", style = MaterialTheme.typography.bodyLarge, color = SlateBlue)
    }
}

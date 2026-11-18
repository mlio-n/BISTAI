package com.muzaffer.bistai.presentation.portfolio

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muzaffer.bistai.core.formatTrCurrency
import com.muzaffer.bistai.core.formatTrLot
import com.muzaffer.bistai.core.formatTrPercent
import com.muzaffer.bistai.core.toCleanBistName
import com.muzaffer.bistai.domain.model.ChartData
import com.muzaffer.bistai.domain.model.ChartTimeRange
import com.muzaffer.bistai.domain.model.PortfolioItem
import com.muzaffer.bistai.domain.model.PortfolioQuickAction
import com.muzaffer.bistai.domain.model.Stock
import com.muzaffer.bistai.presentation.components.AiQuickActionBottomSheet
import com.muzaffer.bistai.presentation.components.ShimmerStockCard
import com.muzaffer.bistai.presentation.stockdetail.BezierLineChartCanvas
import com.muzaffer.bistai.presentation.stockdetail.PlaceholderChartCanvas
import com.muzaffer.bistai.presentation.stockdetail.formatChartTimestamp
import com.muzaffer.bistai.presentation.theme.TrendDown
import com.muzaffer.bistai.presentation.theme.TrendUp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    onStockClick: (String) -> Unit = {},
    viewModel: PortfolioViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var showMarketsSheet by remember { mutableStateOf(false) }

    val ownedItems = remember(uiState.portfolioItems) {
        uiState.portfolioItems.filter { it.lotSize > 0 }
    }

    val ownedStocks = remember(ownedItems, uiState.stocks) {
        ownedItems.mapNotNull { item ->
            val stock = uiState.stocks.find { it.symbol.equals(item.symbol, ignoreCase = true) }
                ?: Stock(
                    symbol = item.symbol,
                    name = item.symbol.toCleanBistName(),
                    currentPrice = item.averageCost,
                    changePercent = 0.0
                )
            stock to item
        }
    }

    val handleFavoriteToggle: (String, String) -> Unit = { symbol, name ->
        val wasFavorite = symbol in uiState.favoriteSymbols
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.toggleFavorite(symbol, name)
        coroutineScope.launch {
            snackbarHostState.showSnackbar(
                if (wasFavorite) "$symbol favorilerden çıkarıldı" else "$symbol favorilere eklendi"
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Portföy",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(TrendUp)
                    )
                    Text(
                        text = "CANLI",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            PullToRefreshBox(
                isRefreshing = uiState.isLoading,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    uiState.isLoading && uiState.stocks.isEmpty() -> LoadingState()
                    uiState.hasError && uiState.stocks.isEmpty() -> ErrorState(
                        message = uiState.errorMessage ?: "Bilinmeyen hata",
                        onRetry = { viewModel.refresh() }
                    )
                    uiState.isEmpty -> EmptyState()
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        item {
                            PortfolioBalanceAndChartSection(
                                totalValue = uiState.currentTotalValue,
                                profitLoss = uiState.totalProfitLoss,
                                profitLossPercentage = uiState.totalProfitLossPercentage,
                                totalInvestment = uiState.totalInvestment,
                                hasOwnedHoldings = ownedStocks.isNotEmpty(),
                                chartData = uiState.portfolioChartData,
                                selectedRange = uiState.selectedChartRange,
                                isChartLoading = uiState.isChartLoading,
                                onRangeSelected = viewModel::selectChartRange
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.executeQuickAction(PortfolioQuickAction.DIVERSIFICATION_RISK)
                                    }
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Portföyümü analiz et",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "Varlıklarım",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }

                        if (ownedStocks.isEmpty()) {
                            item {
                                Text(
                                    text = "Henüz sahip olduğunuz hisse yok.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                                )
                            }
                        } else {
                            items(ownedStocks, key = { (stock, _) -> stock.symbol }) { (stock, item) ->
                                StockRow(
                                    stock = stock,
                                    portfolioItem = item,
                                    onClick = { onStockClick(stock.symbol) },
                                    onLongClick = { handleFavoriteToggle(stock.symbol, stock.name) }
                                )
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outline,
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showMarketsSheet = true }
                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tüm Piyasalar",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${uiState.stocks.size} hisse",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }

    if (showMarketsSheet) {
        MarketStocksBottomSheet(
            stocks = uiState.stocks,
            portfolioItems = uiState.portfolioItems,
            onStockClick = { symbol ->
                showMarketsSheet = false
                onStockClick(symbol)
            },
            onStockLongClick = handleFavoriteToggle,
            onDismiss = { showMarketsSheet = false }
        )
    }

    if (uiState.isQuickActionSheetVisible) {
        AiQuickActionBottomSheet(
            title = uiState.selectedQuickAction?.label ?: "Portföy Analizi",
            subtitle = "Portföy & Piyasa • BISTAI Analitik Rapor",
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

@Composable
private fun PortfolioBalanceAndChartSection(
    totalValue: Double,
    profitLoss: Double,
    profitLossPercentage: Double,
    totalInvestment: Double,
    hasOwnedHoldings: Boolean,
    chartData: ChartData?,
    selectedRange: ChartTimeRange,
    isChartLoading: Boolean,
    onRangeSelected: (ChartTimeRange) -> Unit
) {
    var selectedPointIndex by remember(chartData) { mutableStateOf<Int?>(null) }
    val activePoint = chartData?.points?.let { pts ->
        selectedPointIndex?.takeIf { it in pts.indices }?.let { pts[it] }
    }

    val isProfit = if (hasOwnedHoldings) profitLoss >= 0.0 else (chartData?.isPositive ?: true)
    val trendColor = if (isProfit) TrendUp else TrendDown

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (activePoint != null) {
                val periodDelta = activePoint.closePrice - chartData.firstPrice
                val periodPct = if (chartData.firstPrice > 0.0) {
                    (periodDelta / chartData.firstPrice) * 100.0
                } else {
                    0.0
                }
                val activeColor = if (periodDelta >= 0.0) TrendUp else TrendDown
                Text(
                    text = activePoint.closePrice.formatTrCurrency(),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${periodDelta.formatTrCurrency(includeSign = true)}  ${periodPct.formatTrPercent()}  •  ${formatChartTimestamp(activePoint.timestamp, chartData.range)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = activeColor,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Text(
                    text = totalValue.formatTrCurrency(),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${profitLoss.formatTrCurrency(includeSign = true)}  ${profitLossPercentage.formatTrPercent()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (profitLoss >= 0.0) TrendUp else TrendDown,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (totalInvestment > 0.0) {
                            Text(
                                text = "•  Maliyet ${totalInvestment.formatTrCurrency()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (isChartLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            strokeWidth = 1.5.dp,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp),
            contentAlignment = Alignment.Center
        ) {
            if (chartData != null && chartData.points.size >= 2) {
                BezierLineChartCanvas(
                    chartData = chartData,
                    trendColor = trendColor,
                    selectedPointIndex = selectedPointIndex,
                    onPointSelected = { selectedPointIndex = it },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                PlaceholderChartCanvas(modifier = Modifier.fillMaxSize())
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ChartTimeRange.entries.forEach { range ->
                val isSelected = selectedRange == range
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.background
                        )
                        .clickable { onRangeSelected(range) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = range.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onBackground
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StockRow(
    stock: Stock,
    portfolioItem: PortfolioItem? = null,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val changeColor = if (stock.isBullish) TrendUp else TrendDown
    val displayName = remember(stock.symbol, stock.name) {
        stock.symbol.toCleanBistName(stock.name)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stock.symbol,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                if (portfolioItem != null && portfolioItem.lotSize > 0.0) {
                    Text(
                        text = portfolioItem.lotSize.formatTrLot(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = stock.currentPrice.formatTrCurrency(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stock.changePercent.formatTrPercent(),
                style = MaterialTheme.typography.bodySmall,
                color = changeColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MarketStocksBottomSheet(
    stocks: List<Stock>,
    portfolioItems: List<PortfolioItem>,
    onStockClick: (String) -> Unit,
    onStockLongClick: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusManager = LocalFocusManager.current
    var searchQuery by remember { mutableStateOf("") }

    val filteredStocks = remember(stocks, searchQuery) {
        if (searchQuery.isBlank()) {
            stocks
        } else {
            stocks.filter {
                it.symbol.contains(searchQuery, ignoreCase = true) ||
                it.symbol.toCleanBistName(it.name).contains(searchQuery, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurfaceVariant) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tüm Piyasalar",
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
                    text = "Portföyünüze eklemek veya analiz için hisse seçin",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                placeholder = {
                    Text(
                        "Hisse veya şirket ara...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Ara",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Temizle",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline,
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredStocks, key = { it.symbol }) { stock ->
                    val portfolioItem = portfolioItems.find { it.symbol == stock.symbol }
                    StockRow(
                        stock = stock,
                        portfolioItem = portfolioItem,
                        onClick = { onStockClick(stock.symbol) },
                        onLongClick = { onStockLongClick(stock.symbol, stock.name) }
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        userScrollEnabled = false
    ) {
        items(7) { ShimmerStockCard() }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = TrendDown,
                modifier = Modifier.size(48.dp)
            )
            Text(
                "Bağlantı Hatası",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Tekrar Dene", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Henüz hisse verisi yok",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

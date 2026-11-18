package com.muzaffer.bistai.presentation.stockdetail

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muzaffer.bistai.core.formatTrCurrency
import com.muzaffer.bistai.core.formatTrPercent
import com.muzaffer.bistai.core.toCleanBistName
import com.muzaffer.bistai.domain.model.AiPrediction
import com.muzaffer.bistai.domain.model.ChartData
import com.muzaffer.bistai.domain.model.ChartTimeRange
import com.muzaffer.bistai.domain.model.StockQuickAction
import com.muzaffer.bistai.domain.model.Trend
import com.muzaffer.bistai.domain.repository.AnalysisSource
import com.muzaffer.bistai.presentation.components.AiQuickActionBottomSheet
import com.muzaffer.bistai.presentation.components.ChatBubble
import com.muzaffer.bistai.presentation.theme.TrendDown
import com.muzaffer.bistai.presentation.theme.TrendUp
import com.muzaffer.bistai.presentation.theme.WarningAmber
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val QUICK_QUESTIONS = listOf(
    "Gelecek öngörünüz nedir?",
    "Dolar artarsa ne olur?",
    "Al/Sat için doğru zaman mı?",
    "Son çeyrek performansı?",
    "Rakipleriyle karşılaştır"
)

@OptIn(ExperimentalMaterial3Api::class)
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

    LaunchedEffect(uiState.chatMessages.size) {
        if (uiState.chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.chatMessages.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                symbol = symbol,
                isFavorite = uiState.isFavorite,
                onBack = onBack,
                onToggleFavorite = viewModel::toggleFavorite
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(StockQuickAction.entries) { action ->
                    val isSelected = uiState.selectedQuickAction == action && uiState.isQuickActionSheetVisible
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.executeQuickAction(action) },
                        shape = RoundedCornerShape(8.dp),
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
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurface,
                            iconColor = MaterialTheme.colorScheme.primary,
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = MaterialTheme.colorScheme.outline,
                            selectedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            PullToRefreshBox(
                isRefreshing = uiState.isLoading,
                onRefresh = { viewModel.fetchAnalysis(symbol) },
                modifier = Modifier.weight(1f)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SymbolBadge(
                            symbol = symbol,
                            onAddToPortfolioClick = { showPortfolioSheet = true }
                        )
                    }

                    item {
                        StockChartCard(
                            selectedRange = uiState.selectedChartRange,
                            isLoading = uiState.isChartLoading,
                            chartData = uiState.chartData,
                            errorMessage = uiState.chartError,
                            onRangeSelected = viewModel::selectChartRange,
                            onRetry = { viewModel.fetchChartData(symbol, uiState.selectedChartRange) }
                        )
                    }

                    item {
                        uiState.prediction?.let { pred ->
                            AiPredictionCard(
                                prediction = pred,
                                source = uiState.analysisSource,
                                ageMinutes = uiState.analysisAgeMinutes
                            )
                        }
                    }

                    item {
                        AnimatedContent(
                            targetState = Triple(uiState.isLoading, uiState.errorMessage, uiState.analysis),
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "ai_content"
                        ) { (loading, error, analysis) ->
                            when {
                                loading -> AiLoadingCard()
                                error != null -> AiErrorCard(message = error, onRetry = { viewModel.fetchAnalysis(symbol) })
                                analysis != null -> AiAnalysisCard(text = analysis)
                                else -> AiLoadingCard()
                            }
                        }
                    }

                    if (uiState.chatMessages.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline,
                                thickness = 0.5.dp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "Asistan Sohbeti",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    items(uiState.chatMessages, key = { it.id }) { msg ->
                        ChatBubble(message = msg)
                    }

                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(QUICK_QUESTIONS) { question ->
                    SuggestionChip(
                        onClick = {
                            viewModel.sendChatMessage(question)
                            focusManager.clearFocus()
                        },
                        shape = RoundedCornerShape(8.dp),
                        label = {
                            Text(
                                question,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }

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

@Composable
fun StockChartCard(
    selectedRange: ChartTimeRange,
    isLoading: Boolean,
    chartData: ChartData?,
    errorMessage: String?,
    onRangeSelected: (ChartTimeRange) -> Unit,
    onRetry: () -> Unit
) {
    var selectedPointIndex by remember(chartData) { mutableStateOf<Int?>(null) }

    val activePoint = chartData?.points?.let { pts ->
        selectedPointIndex?.takeIf { it in pts.indices }?.let { pts[it] }
    }

    val isPositive = chartData?.isPositive ?: true
    val trendColor = if (isPositive) TrendUp else TrendDown

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (chartData != null) {
            val displayedPrice = activePoint?.closePrice ?: chartData.lastPrice
            val deltaFromStart = displayedPrice - chartData.firstPrice
            val deltaPercent = if (chartData.firstPrice > 0.0) {
                (deltaFromStart / chartData.firstPrice) * 100.0
            } else {
                0.0
            }
            val badgeColor = if (deltaFromStart >= 0.0) TrendUp else TrendDown

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = displayedPrice.formatTrCurrency(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${deltaFromStart.formatTrCurrency(includeSign = true)}  ${deltaPercent.formatTrPercent()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = badgeColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = activePoint?.let {
                                formatChartTimestamp(it.timestamp, chartData.range)
                            } ?: chartData.range.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Fiyat Grafiği",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        when {
            isLoading && chartData == null -> {
                PlaceholderChartCanvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }

            errorMessage != null && chartData == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = onRetry,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onBackground
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Grafiği Yenile", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            chartData != null && chartData.points.size >= 2 -> {
                BezierLineChartCanvas(
                    chartData = chartData,
                    trendColor = trendColor,
                    selectedPointIndex = selectedPointIndex,
                    onPointSelected = { selectedPointIndex = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "En Düşük: ${chartData.minPrice.formatTrCurrency()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "En Yüksek: ${chartData.maxPrice.formatTrCurrency()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                PlaceholderChartCanvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                )
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

@Composable
fun PlaceholderChartCanvas(modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.outline
    val infiniteTransition = rememberInfiniteTransition(label = "chart_placeholder")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chart_pulse"
    )

    Canvas(modifier = modifier) {
        val midY = size.height * 0.55f
        drawLine(
            color = lineColor.copy(alpha = pulseAlpha),
            start = Offset(0f, midY),
            end = Offset(size.width, midY),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )
    }
}

@Composable
fun BezierLineChartCanvas(
    chartData: ChartData,
    trendColor: Color,
    selectedPointIndex: Int?,
    onPointSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
    val surfaceColor = MaterialTheme.colorScheme.background
    val crosshairColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    val points = chartData.points

    Canvas(
        modifier = modifier
            .pointerInput(points) {
                detectTapGestures { offset ->
                    val idx = ((offset.x / size.width) * (points.size - 1))
                        .roundToInt()
                        .coerceIn(0, points.lastIndex)
                    onPointSelected(if (selectedPointIndex == idx) null else idx)
                }
            }
            .pointerInput(points) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        val idx = ((offset.x / size.width) * (points.size - 1))
                            .roundToInt()
                            .coerceIn(0, points.lastIndex)
                        onPointSelected(idx)
                    },
                    onDragEnd = { onPointSelected(null) },
                    onDragCancel = { onPointSelected(null) },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val idx = ((change.position.x / size.width) * (points.size - 1))
                            .roundToInt()
                            .coerceIn(0, points.lastIndex)
                        onPointSelected(idx)
                    }
                )
            }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val verticalPad = 10.dp.toPx()
        val drawableHeight = (canvasHeight - 2f * verticalPad).coerceAtLeast(1f)

        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
        listOf(verticalPad, canvasHeight / 2f, canvasHeight - verticalPad).forEach { gridY ->
            drawLine(
                color = gridColor,
                start = Offset(0f, gridY),
                end = Offset(canvasWidth, gridY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dashEffect
            )
        }

        val minPrice = chartData.minPrice
        val maxPrice = chartData.maxPrice
        val span = maxPrice - minPrice

        val offsets = points.mapIndexed { index, point ->
            val x = (index.toFloat() / (points.size - 1).coerceAtLeast(1)) * canvasWidth
            val y = if (span <= 0.0) {
                canvasHeight / 2f
            } else {
                val ratio = ((point.closePrice - minPrice) / span).toFloat()
                (canvasHeight - verticalPad) - ratio * drawableHeight
            }
            Offset(x, y)
        }

        val linePath = Path().apply {
            moveTo(offsets.first().x, offsets.first().y)
            for (i in 0 until offsets.lastIndex) {
                val p0 = offsets[i]
                val p1 = offsets[i + 1]
                val controlX = (p0.x + p1.x) / 2f
                cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
            }
        }

        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(offsets.last().x, canvasHeight)
            lineTo(offsets.first().x, canvasHeight)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    trendColor.copy(alpha = 0.25f),
                    trendColor.copy(alpha = 0.03f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = canvasHeight
            )
        )

        drawPath(
            path = linePath,
            color = trendColor,
            style = Stroke(
                width = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        val focusOffset = selectedPointIndex
            ?.takeIf { it in offsets.indices }
            ?.let { offsets[it] }

        if (focusOffset != null) {
            drawLine(
                color = crosshairColor,
                start = Offset(focusOffset.x, 0f),
                end = Offset(focusOffset.x, canvasHeight),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dashEffect
            )
            drawCircle(
                color = trendColor.copy(alpha = 0.22f),
                radius = 10.dp.toPx(),
                center = focusOffset
            )
            drawCircle(
                color = surfaceColor,
                radius = 5.5.dp.toPx(),
                center = focusOffset
            )
            drawCircle(
                color = trendColor,
                radius = 3.5.dp.toPx(),
                center = focusOffset
            )
        }
    }
}

fun formatChartTimestamp(timestampMillis: Long, range: ChartTimeRange): String {
    val pattern = when (range) {
        ChartTimeRange.ONE_DAY, ChartTimeRange.ONE_WEEK -> "dd MMM HH:mm"
        ChartTimeRange.ONE_MONTH, ChartTimeRange.THREE_MONTHS, ChartTimeRange.ONE_YEAR -> "dd MMM yyyy"
    }
    val formatter = SimpleDateFormat(pattern, Locale("tr", "TR"))
    return formatter.format(Date(timestampMillis))
}

@Composable
private fun ChatInputBar(
    symbol: String,
    value: String,
    onValueChange: (String) -> Unit,
    isSending: Boolean,
    onSend: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.background, tonalElevation = 0.dp) {
        Column {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                thickness = 0.5.dp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            "$symbol hakkında bir şey sor...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() })
                )
                FloatingActionButton(
                    onClick = { if (!isSending) onSend() },
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    containerColor = if (isSending) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                    contentColor = if (isSending) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp)
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gönder", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    symbol: String,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Geri",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(
                    symbol,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    symbol.toCleanBistName(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isFavorite) "Favoriden çıkar" else "Favoriye ekle",
                tint = if (isFavorite) TrendDown else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SymbolBadge(
    symbol: String,
    onAddToPortfolioClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    symbol.take(2),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
            Column {
                Text(
                    symbol.toCleanBistName(),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "BIST • Türk Lirası",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Button(
            onClick = onAddToPortfolioClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Portföye Ekle", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AiPredictionCard(
    prediction: AiPrediction,
    source: AnalysisSource? = null,
    ageMinutes: Long? = null
) {
    val trend = prediction.trend
    val trendColor = when (trend) {
        Trend.BULLISH -> TrendUp
        Trend.BEARISH -> TrendDown
        Trend.NEUTRAL -> WarningAmber
    }
    val (sourceLabel, sourceColor) = when (source) {
        AnalysisSource.FIREBASE -> "Firebase" to TrendUp
        AnalysisSource.LOCAL_CACHE -> "Önbellek" to WarningAmber
        AnalysisSource.FRESH_API -> "Canlı Analiz" to MaterialTheme.colorScheme.onSurfaceVariant
        null -> "" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    val ageText = ageMinutes?.let {
        when {
            it < 60   -> "$it dk önce"
            it < 1440 -> "${it / 60} saat önce"
            else      -> "${it / 1440} gün önce"
        }
    } ?: ""

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            thickness = 0.5.dp
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    "BISTAI Tahmini",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                trend.label,
                color = trendColor,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium
            )
        }

        if (source != null) {
            Text(
                if (ageText.isNotEmpty()) "$sourceLabel • $ageText" else sourceLabel,
                style = MaterialTheme.typography.labelSmall,
                color = sourceColor
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Güven Skoru",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "%${prediction.confidenceScore}",
                    style = MaterialTheme.typography.labelSmall,
                    color = trendColor,
                    fontWeight = FontWeight.Bold
                )
            }
            LinearProgressIndicator(
                progress = { prediction.confidenceScore / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .height(6.dp),
                color = trendColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        if (prediction.targetLow > 0 || prediction.targetHigh > 0) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Tahmini Hedef Aralık",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${prediction.targetLow.formatTrCurrency()} — ${prediction.targetHigh.formatTrCurrency()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Text(
            prediction.reasoning,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 20.sp
        )
    }
}

@Composable
fun AiAnalysisCard(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "Analiz Notu",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 22.sp
        )
        Text(
            "Bilgi amaçlıdır, yatırım tavsiyesi değildir.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AiLoadingCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 2.dp,
            modifier = Modifier.size(20.dp)
        )
        Text(
            "Analiz hazırlanıyor...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AiErrorCard(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = WarningAmber,
                modifier = Modifier.size(20.dp)
            )
            Text(
                "Analiz Yapılamadı",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(
            onClick = onRetry,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onBackground
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Tekrar Dene", fontWeight = FontWeight.SemiBold)
        }
    }
}

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
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurfaceVariant) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Portföye Ekle: $symbol",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = lotSize,
                onValueChange = { lotSize = it },
                label = { Text("Lot (Adet)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = averageCost,
                onValueChange = { averageCost = it },
                label = { Text("Alış Maliyeti (₺)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )

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
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Portföye Kaydet",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

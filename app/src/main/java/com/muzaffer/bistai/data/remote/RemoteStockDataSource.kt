package com.muzaffer.bistai.data.remote

import com.muzaffer.bistai.core.toCleanBistName
import com.muzaffer.bistai.data.remote.dto.StockDto
import com.muzaffer.bistai.domain.model.ChartData
import com.muzaffer.bistai.domain.model.ChartPoint
import com.muzaffer.bistai.domain.model.ChartTimeRange
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class RemoteStockDataSource @Inject constructor(
    private val api: YfApiService
) {
    private val defaultBistSymbols = listOf(
        "THYAO.IS", "GARAN.IS", "SISE.IS", "YKBNK.IS", "KCHOL.IS",
        "EREGL.IS", "AKBNK.IS", "ASELS.IS", "BIMAS.IS", "TUPRS.IS",
        "PGSUS.IS", "TOASO.IS", "FROTO.IS", "SAHOL.IS", "TAVHL.IS"
    )

    suspend fun getStocks(): List<StockDto> {
        return fetchCharts(defaultBistSymbols)
    }

    suspend fun getStockDetail(symbol: String): StockDto? {
        return fetchSingleChart(toYfSymbol(symbol))
    }

    suspend fun getBatchStocks(symbols: List<String>): List<StockDto> {
        return fetchCharts(symbols.map(::toYfSymbol))
    }

    suspend fun getStockChart(symbol: String, range: ChartTimeRange): ChartData {
        val yfSymbol = toYfSymbol(symbol)
        val response = api.getChartQuote(
            symbol = yfSymbol,
            interval = range.apiInterval,
            range = range.apiRange
        )
        val result = response.chart?.result?.firstOrNull()
            ?: throw IllegalStateException("$symbol grafik verisi bulunamadı")
        val meta = result.meta
        val timestamps = result.timestamp.orEmpty()
        val closes = result.indicators?.quote?.firstOrNull()?.close.orEmpty()
        val points = timestamps.zip(closes) { ts, close ->
            if (close != null && close > 0.0 && !close.isNaN()) {
                val millis = if (ts < 100_000_000_000L) ts * 1000L else ts
                ChartPoint(timestamp = millis, closePrice = close)
            } else {
                null
            }
        }.filterNotNull()

        val cleanSymbol = (meta?.symbol ?: yfSymbol).removeSuffix(".IS")
        val currentPrice = meta?.regularMarketPrice ?: points.lastOrNull()?.closePrice ?: 0.0
        val previousClose = meta?.chartPreviousClose ?: meta?.previousClose

        return ChartData(
            symbol = cleanSymbol,
            range = range,
            points = points,
            currentPrice = currentPrice,
            previousClose = previousClose
        )
    }

    private fun toYfSymbol(symbol: String): String {
        val normalized = symbol.trim().uppercase()
        return if (normalized.endsWith(".IS")) normalized else "$normalized.IS"
    }

    private suspend fun fetchCharts(symbols: List<String>): List<StockDto> = coroutineScope {
        if (symbols.isEmpty()) return@coroutineScope emptyList()

        val results = symbols.map { yfSymbol ->
            async { runCatching { fetchSingleChart(yfSymbol) } }
        }.awaitAll()

        val stocks = results.mapNotNull { it.getOrNull() }
        if (stocks.isEmpty()) {
            results.firstNotNullOfOrNull { it.exceptionOrNull() }?.let { throw it }
        }
        stocks
    }

    private suspend fun fetchSingleChart(yfSymbol: String): StockDto? {
        val response = api.getChartQuote(yfSymbol)
        val meta = response.chart?.result?.firstOrNull()?.meta ?: return null
        val rawSymbol = meta.symbol ?: yfSymbol
        val cleanSymbol = rawSymbol.removeSuffix(".IS")
        val currentPrice = meta.regularMarketPrice ?: 0.0
        val previousClose = meta.chartPreviousClose ?: meta.previousClose
        val changePercent = meta.regularMarketChangePercent ?: run {
            if (previousClose != null && previousClose > 0.0 && currentPrice > 0.0) {
                ((currentPrice - previousClose) / previousClose) * 100.0
            } else {
                0.0
            }
        }

        return StockDto(
            symbol = cleanSymbol,
            name = cleanSymbol.toCleanBistName(meta.longName ?: meta.shortName),
            currentPrice = currentPrice,
            changePercent = changePercent,
            previousClose = previousClose,
            marketCap = null,
            volume = meta.regularMarketVolume
        )
    }
}

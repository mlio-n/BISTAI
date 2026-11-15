package com.muzaffer.bistai.data.remote

import com.muzaffer.bistai.data.remote.dto.StockDto
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

    /**
     * BIST'teki hisselerin genel bir listesini (portföy izleme ekranı için) çeker.
     * Bu sabit sembolleri daha sonra veritabanı kontrollü de yapabiliriz.
     */
    suspend fun getStocks(): List<StockDto> {
        return fetchCharts(defaultBistSymbols)
    }

    suspend fun getStockDetail(symbol: String): StockDto? {
        return fetchSingleChart(toYfSymbol(symbol))
    }

    suspend fun getBatchStocks(symbols: List<String>): List<StockDto> {
        return fetchCharts(symbols.map(::toYfSymbol))
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
        // Sembolün sonundaki ".IS" ekini kaldırarak (THYAO.IS -> THYAO) UI'da temiz gösterelim
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
            name = meta.longName ?: meta.shortName ?: cleanSymbol,
            currentPrice = currentPrice,
            changePercent = changePercent,
            previousClose = previousClose,
            marketCap = null,
            volume = meta.regularMarketVolume
        )
    }
}

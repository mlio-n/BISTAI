package com.muzaffer.bistai.data.remote

import android.util.Log
import com.muzaffer.bistai.data.remote.dto.StockDto
import com.muzaffer.bistai.data.remote.dto.YahooQuoteResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Yahoo Finance API'sini kullanarak gerçek borsa verisi döndüren veri kaynağı.
 *
 * BIST hisseleri için ".IS" suffix eklenir (örn. THYAO → THYAO.IS).
 * Altın/Gümüş için USDTRY kuru kullanılarak TL/gram hesaplaması yapılır.
 * API başarısız olursa çağıran katman [FakeStockDataSource]'a düşer.
 */
@Singleton
class RealStockDataSource @Inject constructor(
    private val yahooFinanceApiService: YahooFinanceApiService
) {
    companion object {
        private const val TAG = "RealStockDataSource"

        /**
         * Dahili sembol → (Yahoo sembolü, Türkçe isim) eşleştirmesi.
         * Null isim: Yahoo'dan gelen shortName kullanılır.
         */
        private val SYMBOL_MAP: Map<String, Pair<String, String>> = mapOf(
            // ─── BIST Hisseleri (.IS suffix) ────────────────────────────────
            "THYAO"  to ("THYAO.IS"  to "Türk Hava Yolları"),
            "GARAN"  to ("GARAN.IS"  to "Garanti BBVA"),
            "SISE"   to ("SISE.IS"   to "Şişe Cam"),
            "YKBNK"  to ("YKBNK.IS"  to "Yapı Kredi Bankası"),
            "KCHOL"  to ("KCHOL.IS"  to "Koç Holding"),
            "EREGL"  to ("EREGL.IS"  to "Ereğli Demir Çelik"),
            "AKBNK"  to ("AKBNK.IS"  to "Akbank"),
            "ASELS"  to ("ASELS.IS"  to "Aselsan"),
            "BIMAS"  to ("BIMAS.IS"  to "BİM Birleşik Mağazalar"),
            "TUPRS"  to ("TUPRS.IS"  to "Tüpraş"),
            "PGSUS"  to ("PGSUS.IS"  to "Pegasus Hava Yolları"),
            "TOASO"  to ("TOASO.IS"  to "Tofaş Otomobil"),
            "FROTO"  to ("FROTO.IS"  to "Ford Otosan"),
            "SAHOL"  to ("SAHOL.IS"  to "Sabancı Holding"),
            "TAVHL"  to ("TAVHL.IS"  to "TAV Havalimanları"),
            // ─── Emtia (USD/oz vadeli işlem) ────────────────────────────────
            "ALTIN"  to ("GC=F"      to "Altın (TL/Gram)"),
            "GUMUS"  to ("SI=F"      to "Gümüş (TL/Gram)"),
            // ─── Kripto ─────────────────────────────────────────────────────
            "BTCUSD" to ("BTC-USD"   to "Bitcoin / USD"),
            // ─── Döviz ──────────────────────────────────────────────────────
            "USDTRY" to ("USDTRY=X"  to "Dolar / Türk Lirası")
        )

        /** Troy ons → gram dönüşüm sabiti */
        private const val TROY_OZ_TO_GRAM = 31.1035
    }

    /**
     * Tüm varlıkların anlık fiyat listesini döner.
     * Altın ve Gümüş için USDTRY kullanılarak TL/gram fiyatı hesaplanır.
     * @throws Exception API çağrısı başarısız olursa
     */
    suspend fun getStocks(): List<StockDto> {
        val yahooSymbols = SYMBOL_MAP.values.joinToString(",") { it.first }
        Log.d(TAG, "📤 Yahoo Finance isteği: $yahooSymbols")
        return fetchByYahooSymbols(yahooSymbols)
    }

    /** Sembol bazında tek varlık detayını döner. */
    suspend fun getStockDetail(symbol: String): StockDto? {
        val mapping = SYMBOL_MAP[symbol.uppercase()] ?: return null
        // USDTRY oranı emtia dönüşümü için de gerekebilir; altın/gümüş ise birlikte çekilir
        val yahooSymbols = if (symbol.uppercase() in setOf("ALTIN", "GUMUS")) {
            "${mapping.first},USDTRY=X"
        } else {
            mapping.first
        }
        return fetchByYahooSymbols(yahooSymbols).firstOrNull { it.symbol == symbol.uppercase() }
    }

    /** Belirtilen sembollere ait varlıkları döner. */
    suspend fun getBatchStocks(symbols: List<String>): List<StockDto> {
        val upperSymbols = symbols.map { it.uppercase() }
        val containsMetals = upperSymbols.any { it == "ALTIN" || it == "GUMUS" }
        val yahooSymbols = buildSet {
            upperSymbols.forEach { s -> SYMBOL_MAP[s]?.let { add(it.first) } }
            // Altın/Gümüş TL dönüşümü için USDTRY de gerekli
            if (containsMetals) add("USDTRY=X")
        }.joinToString(",")
        return fetchByYahooSymbols(yahooSymbols).filter { it.symbol in upperSymbols }
    }

    /**
     * Verilen Yahoo Finance sembollerini tek bir API çağrısıyla getirir
     * ve [StockDto] listesine dönüştürür.
     */
    private suspend fun fetchByYahooSymbols(yahooSymbols: String): List<StockDto> {
        val response = yahooFinanceApiService.getQuotes(yahooSymbols)

        if (!response.isSuccessful || response.body() == null) {
            throw Exception("Yahoo Finance API hatası: ${response.code()}")
        }

        val results = response.body()!!.quoteResponse.result ?: emptyList()
        Log.d(TAG, "📥 Yahoo Finance: ${results.size} sembol alındı")

        val resultByYahooSymbol: Map<String, YahooQuoteResult> =
            results.associateBy { it.symbol }

        val usdTryRate = resultByYahooSymbol["USDTRY=X"]?.regularMarketPrice ?: 0.0

        val yahooToInternal: Map<String, String> =
            SYMBOL_MAP.entries.associate { (internal, pair) -> pair.first to internal }

        return results.mapNotNull { quote ->
            val internalSymbol = yahooToInternal[quote.symbol] ?: return@mapNotNull null
            val displayName    = SYMBOL_MAP[internalSymbol]?.second ?: quote.shortName ?: internalSymbol

            val price         = quote.regularMarketPrice ?: return@mapNotNull null
            val prevClose     = quote.regularMarketPreviousClose ?: price
            val changePercent = quote.regularMarketChangePercent ?: 0.0

            val (finalPrice, finalPrevClose) = when (internalSymbol) {
                "ALTIN", "GUMUS" -> if (usdTryRate > 0) {
                    Pair(
                        price     / TROY_OZ_TO_GRAM * usdTryRate,
                        prevClose / TROY_OZ_TO_GRAM * usdTryRate
                    )
                } else {
                    Pair(price, prevClose)
                }
                else -> Pair(price, prevClose)
            }

            StockDto(
                symbol        = internalSymbol,
                name          = displayName,
                currentPrice  = (finalPrice * 100).toLong() / 100.0,
                changePercent = (changePercent * 100).toLong() / 100.0,
                previousClose = (finalPrevClose * 100).toLong() / 100.0,
                marketCap     = quote.marketCap,
                volume        = quote.regularMarketVolume
            )
        }
    }
}

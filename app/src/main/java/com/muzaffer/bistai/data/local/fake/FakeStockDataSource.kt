package com.muzaffer.bistai.data.local.fake

import com.muzaffer.bistai.data.remote.dto.StockDto
import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlin.random.Random

/**
 * Gerçek borsa API'si olmadan uygulamayı test etmek için kullanılan
 * sahte veri kaynağı. Ağ gecikmesini simüle etmek için `delay()` kullanır.
 *
 * Gerçek API hazır olduğunda NetworkDataSource implementasyonu bu sınıfın yerini alacak.
 */
class FakeStockDataSource @Inject constructor() {

    // Türkiye'nin en likit BIST hisseleri — başlangıç tabanı
    private val baseStocks = listOf(
        Triple("THYAO", "Türk Hava Yolları", 285.50),
        Triple("GARAN", "Garanti BBVA", 102.30),
        Triple("SISE",  "Şişe Cam",         48.70),
        Triple("YKBNK", "Yapı Kredi Bankası",21.60),
        Triple("KCHOL", "Koç Holding",      195.40),
        Triple("EREGL", "Ereğli Demir Çelik",52.80),
        Triple("AKBNK", "Akbank",            37.20),
        Triple("ASELS", "Aselsan",          100.50),
        Triple("BIMAS", "BİM Birleşik Mağazalar", 492.00),
        Triple("TUPRS", "Tüpraş",           189.90),
        Triple("PGSUS", "Pegasus Hava Yolları", 1120.00),
        Triple("TOASO", "Tofaş Oto Fabrikalası", 247.00),
        Triple("FROTO", "Ford Otosan",       1034.50),
        Triple("SAHOL", "Sabancı Holding",    88.10),
        Triple("TAVHL", "TAV Havalimanları",  135.70)
    )

    /**
     * Rastgele fiyat değişimleri uygulayarak gerçekçi hisse verileri döner.
     * Her çağrıda farklı değerler üretir.
     */
    suspend fun getStocks(): List<StockDto> {
        delay(800) // Ağ gecikmesi simülasyonu
        return baseStocks.map { (symbol, name, basePrice) ->
            val changePercent = Random.nextDouble(-5.0, 5.0)
            val currentPrice = basePrice * (1 + changePercent / 100)
            StockDto(
                symbol        = symbol,
                name          = name,
                currentPrice  = String.format("%.2f", currentPrice).toDouble(),
                changePercent = String.format("%.2f", changePercent).toDouble(),
                previousClose = basePrice,
                marketCap     = (currentPrice * Random.nextLong(100_000_000, 5_000_000_000)).toLong(),
                volume        = Random.nextLong(1_000_000, 50_000_000)
            )
        }
    }

    /** Sembol bazında tek hisse döner. */
    suspend fun getStockDetail(symbol: String): StockDto? {
        delay(400)
        return getStocks().firstOrNull { it.symbol == symbol }
    }

    /** Belirtilen sembollere ait hisseleri döner. */
    suspend fun getBatchStocks(symbols: List<String>): List<StockDto> {
        delay(600)
        return getStocks().filter { it.symbol in symbols }
    }
}

package com.muzaffer.bistai.data.remote

import android.util.Log
import com.muzaffer.bistai.BuildConfig
import com.muzaffer.bistai.domain.model.NewsItem
import com.muzaffer.bistai.domain.model.NewsSentiment
import javax.inject.Inject
import javax.inject.Singleton

/**
 * GNews API'sini kullanarak gerçek Türkçe haber başlıkları döndüren veri kaynağı.
 *
 * API anahtarı gerektirir: local.properties dosyasına `GNEWS_API_KEY=...` ekleyin.
 * Ücretsiz plan: günde 100 istek, en fazla 10 makale/istek.
 * Anahtar yoksa veya API çağrısı başarısız olursa boş liste döner;
 * çağıran katman ([NewsRepositoryImpl]) o durumda fake veriye düşer.
 *
 * Anahtar almak için: https://gnews.io
 */
@Singleton
class RealNewsDataSource @Inject constructor(
    private val gNewsApiService: GNewsApiService
) {
    companion object {
        private const val TAG = "RealNewsDataSource"

        /** Haber başlığında geçince pozitif duygu puanı artıran anahtar kelimeler. */
        private val POSITIVE_KEYWORDS = listOf(
            "artış", "yüksel", "rekor", "kazanç", "büyüme", "güçlü", "rallye",
            "tavan", "başarı", "kâr", "talep", "giriş", "destek", "toparlan",
            "increase", "rise", "growth", "gain", "strong", "record", "rally"
        )

        /** Haber başlığında geçince negatif duygu puanı artıran anahtar kelimeler. */
        private val NEGATIVE_KEYWORDS = listOf(
            "düşüş", "gerileme", "kayıp", "baskı", "zayıf", "endişe", "risk",
            "taban", "zarar", "çıkış", "yavaşlama", "kriz", "satış", "bozul",
            "decrease", "fall", "loss", "pressure", "weak", "crisis", "sell"
        )

        /** Dahili sembol → GNews arama sorgusu eşleştirmesi. */
        private val SEARCH_QUERIES: Map<String, String> = mapOf(
            "THYAO"  to "Türk Hava Yolları THY borsa",
            "GARAN"  to "Garanti BBVA banka hisse",
            "SISE"   to "Şişecam Şişe Cam hisse",
            "YKBNK"  to "Yapı Kredi banka hisse",
            "KCHOL"  to "Koç Holding hisse borsa",
            "EREGL"  to "Ereğli Demir Çelik hisse",
            "AKBNK"  to "Akbank hisse borsa",
            "ASELS"  to "Aselsan savunma hisse",
            "BIMAS"  to "BİM market hisse borsa",
            "TUPRS"  to "Tüpraş petrol hisse",
            "PGSUS"  to "Pegasus havayolu hisse",
            "TOASO"  to "Tofaş otomobil hisse",
            "FROTO"  to "Ford Otosan hisse borsa",
            "SAHOL"  to "Sabancı Holding hisse",
            "TAVHL"  to "TAV Havalimanları hisse",
            "ALTIN"  to "altın fiyat TL gram",
            "GUMUS"  to "gümüş fiyat TL gram",
            "BTCUSD" to "Bitcoin BTC kripto fiyat",
            "USDTRY" to "dolar TL kur TCMB"
        )
    }

    /** BuildConfig.GNEWS_API_KEY tanımlı mı? */
    val isApiKeySet: Boolean
        get() = BuildConfig.GNEWS_API_KEY.isNotBlank()

    /**
     * Verilen sembol için en fazla 5 gerçek haber başlığı döner.
     * API anahtarı yoksa veya hata oluşursa boş liste döner.
     */
    suspend fun getNewsForAsset(symbol: String): List<NewsItem> {
        if (!isApiKeySet) {
            Log.w(TAG, "⚠️ GNEWS_API_KEY tanımlı değil — sahte veriye düşülüyor")
            return emptyList()
        }

        val query = SEARCH_QUERIES[symbol.uppercase()]
            ?: "${symbol.uppercase()} hisse borsa"

        return try {
            val response = gNewsApiService.searchNews(
                query  = query,
                lang   = "tr",
                max    = 5,
                apiKey = BuildConfig.GNEWS_API_KEY
            )

            if (!response.isSuccessful || response.body() == null) {
                Log.e(TAG, "❌ GNews API hatası: ${response.code()}")
                return emptyList()
            }

            val articles = response.body()!!.articles
            Log.d(TAG, "✅ $symbol için ${articles.size} haber alındı")

            articles.map { article ->
                NewsItem(
                    headline  = article.title,
                    source    = article.source.name,
                    sentiment = analyzeSentiment(
                        article.title + " " + (article.description ?: "")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "💥 GNews exception: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Metin içeriğine göre basit kelime tabanlı duygu analizi yapar.
     * Pozitif kelime sayısı > negatif → POSITIVE, tersi → NEGATIVE, eşit → NEUTRAL.
     */
    private fun analyzeSentiment(text: String): NewsSentiment {
        val lower = text.lowercase()
        val pos   = POSITIVE_KEYWORDS.count { lower.contains(it) }
        val neg   = NEGATIVE_KEYWORDS.count { lower.contains(it) }
        return when {
            pos > neg -> NewsSentiment.POSITIVE
            neg > pos -> NewsSentiment.NEGATIVE
            else      -> NewsSentiment.NEUTRAL
        }
    }
}

package com.muzaffer.bistai.data.remote

import android.util.Log
import android.util.Xml
import com.muzaffer.bistai.domain.model.NewsItem
import com.muzaffer.bistai.domain.model.NewsSentiment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google News RSS feed'ini kullanarak gerçek Türkçe haber başlıkları döndüren veri kaynağı.
 *
 * API key gerektirmez — tamamen ücretsiz ve sınırsız.
 * RSS feed URL formatı: https://news.google.com/rss/search?q=QUERY&hl=tr&gl=TR&ceid=TR:tr
 * Android'in built-in [XmlPullParser] ile parse edilir.
 * Hata durumunda boş liste döner; çağıran katman ([NewsRepositoryImpl]) fake veriye düşer.
 */
@Singleton
class GoogleNewsRssDataSource @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "GoogleNewsRssDataSource"
        private const val MAX_ITEMS = 5

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

        /** Dahili sembol → Google News RSS arama sorgusu eşleştirmesi. */
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

    /** Google News RSS API key gerektirmez; her zaman true döner. */
    val isApiKeySet: Boolean = true

    /**
     * Verilen sembol için en fazla 5 gerçek haber başlığı döner.
     * Hata oluşursa boş liste döner.
     */
    suspend fun getNewsForAsset(symbol: String): List<NewsItem> = withContext(Dispatchers.IO) {
        val query = SEARCH_QUERIES[symbol.uppercase()]
            ?: "${symbol.uppercase()} hisse borsa"

        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "https://news.google.com/rss/search?q=$encodedQuery&hl=tr&gl=TR&ceid=TR:tr"

        try {
            val request = Request.Builder()
                .url(url)
                .build()

            val responseBody = okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "❌ Google News RSS hatası: ${response.code}")
                    return@withContext emptyList()
                }
                response.body?.string() ?: run {
                    Log.e(TAG, "❌ Google News RSS boş yanıt")
                    return@withContext emptyList()
                }
            }

            val items = parseRss(responseBody)
            Log.d(TAG, "✅ $symbol için ${items.size} haber alındı")
            items
        } catch (e: Exception) {
            Log.e(TAG, "💥 Google News RSS exception: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * RSS XML içeriğini parse ederek [NewsItem] listesi döner.
     * Her <item> içindeki <title> ve <source> etiketleri okunur.
     */
    private fun parseRss(xml: String): List<NewsItem> {
        val items = mutableListOf<NewsItem>()
        val parser: XmlPullParser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(xml.reader())

        var title: String? = null
        var source: String? = null
        var insideItem = false
        var currentTag: String? = null

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT && items.size < MAX_ITEMS) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    if (currentTag == "item") {
                        insideItem = true
                        title = null
                        source = null
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideItem) {
                        val text = parser.text?.trim()
                        if (!text.isNullOrEmpty()) {
                            when (currentTag) {
                                "title"  -> title  = text
                                "source" -> source = text
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "item" && insideItem) {
                        val headline = title
                        if (!headline.isNullOrBlank()) {
                            items.add(
                                NewsItem(
                                    headline  = headline,
                                    source    = source ?: "Google News",
                                    sentiment = analyzeSentiment(headline)
                                )
                            )
                        }
                        insideItem = false
                    }
                    currentTag = null
                }
            }
            eventType = parser.next()
        }

        return items
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

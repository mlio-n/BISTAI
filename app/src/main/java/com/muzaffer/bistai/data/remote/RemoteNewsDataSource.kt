package com.muzaffer.bistai.data.remote

import com.muzaffer.bistai.domain.model.NewsItem
import com.muzaffer.bistai.domain.model.NewsSentiment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

/**
 * TRT Haber Ekonomi RSS veya benzeri bir XML kaynağından güncel piyasa
 * haberlerini çeken Remote Data Source sınıfıdır.
 * Harici bir kütüphane (Retrofit XML converter vs.) kullanmadan Android'in 
 * dahili XmlPullParser'ını kullanır.
 */
class RemoteNewsDataSource @Inject constructor() {

    // TRT Haber Ekonomi RSS Servisi
    private val RSS_URL = "https://www.trthaber.com/ekonomi_articles.rss"

    suspend fun getNewsForAsset(symbol: String): List<NewsItem> = withContext(Dispatchers.IO) {
        val newsList = mutableListOf<NewsItem>()
        try {
            val url = URL(RSS_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            val inputStream: InputStream = connection.inputStream

            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(inputStream, "UTF-8")

            var eventType = parser.eventType
            var currentTitle: String? = null
            var insideItem = false

            while (eventType != XmlPullParser.END_DOCUMENT && newsList.size < 6) { // En fazla 6 haber al
                val tagName = parser.name

                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tagName.equals("item", ignoreCase = true)) {
                            insideItem = true
                        } else if (tagName.equals("title", ignoreCase = true) && insideItem) {
                            currentTitle = parser.nextText().trim()
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tagName.equals("item", ignoreCase = true)) {
                            if (currentTitle != null) {
                                // Şimdilik karmaşık duygu analizi yerine NewsSentiment.NEUTRAL atıyoruz.
                                newsList.add(
                                    NewsItem(
                                        headline = currentTitle,
                                        source = "TRT Haber",
                                        sentiment = NewsSentiment.NEUTRAL
                                    )
                                )
                            }
                            insideItem = false
                            currentTitle = null
                        }
                    }
                }
                eventType = parser.next()
            }
            inputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
            // Hata durumunda boş liste döner, UI "haber yok" olarak işler
        }
        
        // Eğer sembole (örn. THYAO) özel bir filtre istenirse burada filter uygulanabilir. 
        // Şimdilik genel ekonomi haberini döndürüyoruz.
        return@withContext newsList
    }

    /** Güncel makro ekonomik bağlam. Şimdilik sabit ama ileride buraya TCMB vs. apileri de bağlanabilir */
    fun getMacroContext(): String = """
        Bu haberler gerçek zamanlı TRT Finans/Ekonomi RSS servisinden alınmıştır.
        Yapay zeka (BISTAI) bu son dakika haberlerini hesaba katarak analiz yapmalıdır.
    """.trimIndent()
}

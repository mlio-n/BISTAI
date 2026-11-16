package com.muzaffer.bistai.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.muzaffer.bistai.data.local.dao.AnalysisCacheDao
import com.muzaffer.bistai.data.local.entity.AnalysisCacheEntity
import com.muzaffer.bistai.data.remote.AiApiService
import com.muzaffer.bistai.domain.model.AiPrediction
import com.muzaffer.bistai.domain.model.NewsItem
import com.muzaffer.bistai.domain.model.PortfolioItem
import com.muzaffer.bistai.domain.model.PortfolioQuickAction
import com.muzaffer.bistai.domain.model.Stock
import com.muzaffer.bistai.domain.model.StockQuickAction
import com.muzaffer.bistai.domain.repository.AnalysisRepository
import com.muzaffer.bistai.domain.repository.AnalysisResult
import com.muzaffer.bistai.domain.repository.AnalysisSource
import kotlinx.coroutines.tasks.await
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Öncelik sırası:
 * 1. Firebase Firestore → analyses/{symbol} (< 24 saat ise kullan)
 * 2. Gemini API → Firebase'e + Room'a yaz
 * 3. API hatası (kota aşıldı vb.) → Room cache (eski bile olsa)
 *
 * Sonuç: Tüm kullanıcılar günde sadece 1 API çağrısı paylaşır.
 */
@Singleton
class AnalysisRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val cacheDao: AnalysisCacheDao,
    private val aiService: AiApiService
) : AnalysisRepository {

    companion object {
        private const val TAG = "BISTAI_ANALYSIS"
        private const val COLLECTION = "analyses"
        private const val TTL_MS = 24 * 60 * 60 * 1000L  // 24 saat
    }

    override suspend fun getAnalysis(
        symbol: String,
        newsItems: List<NewsItem>,
        macroContext: String
    ): Result<AnalysisResult> {

        // ── 1. Firebase kontrol ─────────────────────────────────────────
        try {
            val doc = firestore.collection(COLLECTION)
                .document(symbol.uppercase())
                .get()
                .await()

            if (doc.exists()) {
                val generatedAt = doc.getLong("generatedAt") ?: 0L
                val age = System.currentTimeMillis() - generatedAt

                if (age < TTL_MS) {
                    Log.d(TAG, "✅ Firebase'den alındı: $symbol (${age / 3600000}s önce)")
                    val prediction = mapDocToPrediction(doc.data!!)
                    // Room'a da kaydet (offline yedek)
                    cacheDao.upsert(AnalysisCacheEntity.from(symbol, prediction, "FIREBASE"))
                    return Result.success(
                        AnalysisResult(prediction, AnalysisSource.FIREBASE, generatedAt)
                    )
                } else {
                    Log.d(TAG, "⚠️ Firebase verisi eski ($symbol), yenileniyor...")
                }
            } else {
                Log.d(TAG, "ℹ️ Firebase'de $symbol bulunamadı, yeni analiz yapılıyor")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firebase okuma hatası: ${e.message}")
        }

        // ── 2. Gemini API çağrısı ───────────────────────────────────────
        val apiResult = aiService.analyzeAsset(
            symbol = symbol,
            newsItems = newsItems,
            macroContext = macroContext
        )

        apiResult.onSuccess { prediction ->
            val now = System.currentTimeMillis()
            Log.d(TAG, "✅ Gemini API başarılı, Firebase + Room'a yazılıyor")
            saveToFirebase(symbol, prediction)
            cacheDao.upsert(AnalysisCacheEntity.from(symbol, prediction, "API"))
            return Result.success(
                AnalysisResult(prediction, AnalysisSource.FRESH_API, now)
            )
        }

        // ── 3. API başarısız → Room cache ─────────────────────────────
        Log.w(TAG, "⚠️ API başarısız, Room cache deneniyor: $symbol")
        val cached = cacheDao.getBySymbol(symbol.uppercase())
        if (cached != null) {
            Log.d(TAG, "✅ Room cache bulundu (${(System.currentTimeMillis() - cached.generatedAt) / 3600000}s önce)")
            return Result.success(
                AnalysisResult(cached.toAiPrediction(), AnalysisSource.LOCAL_CACHE, cached.generatedAt)
            )
        }

        // Son çare: orijinal API hatasını döndür
        return Result.failure(apiResult.exceptionOrNull() ?: Exception("Analiz alınamadı"))
    }

    override suspend fun runStockQuickAction(
        action: StockQuickAction,
        symbol: String,
        stock: Stock?,
        portfolioItem: PortfolioItem?,
        newsItems: List<NewsItem>,
        macroContext: String,
        prediction: AiPrediction?
    ): Result<String> {
        val priceContext = stock?.let {
            val prev = it.previousClose?.let { p -> String.format(Locale.US, "%.2f TL", p) } ?: "Bilinmiyor"
            val vol = it.volume?.toString() ?: "Bilinmiyor"
            """
            |Güncel Fiyat: ${String.format(Locale.US, "%.2f TL", it.currentPrice)}
            |Günlük Değişim: ${it.formattedChangePercent}
            |Önceki Kapanış: $prev
            |Hacim: $vol
            """.trimMargin()
        } ?: "Güncel piyasa fiyat verisi anlık olarak alınamadı."

        val positionContext = portfolioItem?.takeIf { it.lotSize > 0 }?.let { item ->
            val currentPrice = stock?.currentPrice ?: item.averageCost
            val currentValue = currentPrice * item.lotSize
            val pnl = currentValue - item.totalInvestment
            val pnlPct = if (item.totalInvestment > 0) (pnl / item.totalInvestment) * 100.0 else 0.0
            """
            |Lot: ${item.lotSize}
            |Ortalama Maliyet: ${String.format(Locale.US, "%.2f TL", item.averageCost)}
            |Toplam Maliyet: ${String.format(Locale.US, "%.2f TL", item.totalInvestment)}
            |Güncel Değer: ${String.format(Locale.US, "%.2f TL", currentValue)}
            |Net Kâr/Zarar: ${String.format(Locale.US, "%+.2f TL (%+.2f%%)", pnl, pnlPct)}
            """.trimMargin()
        } ?: "Kullanıcı portföyünde bu hisseye ait açık pozisyon bulunmuyor."

        val predContext = prediction?.let {
            "Trend: ${it.trend.label} | Güven: %${it.confidenceScore} | Hedef Aralık: ${it.targetLow} - ${it.targetHigh} TL | Özet: ${it.reasoning}"
        } ?: "Henüz kayıtlı teknik/temel tahmin özeti yok."

        val newsBlock = if (newsItems.isEmpty()) {
            "Güncel haber başlığı bulunamadı."
        } else {
            newsItems.joinToString("\n") { "- [${it.source}] ${it.headline}" }
        }

        val taskDirective = when (action) {
            StockQuickAction.QUICK_SUMMARY ->
                "$symbol için güncel fiyat, trend, haber akışı ve varsa portföy pozisyonunu tek bakışta özetle."
            StockQuickAction.NEWS_ANALYSIS ->
                "Son ekonomi ve piyasa haberlerinin $symbol hissesi ve sektörü üzerindeki olası etkilerini tablo ve maddelerle analiz et."
            StockQuickAction.RISK_AND_LEVELS ->
                "$symbol için volatilite, kritik destek/direnç veya takip seviyeleri ve yatırımcı risklerini analiz et."
            StockQuickAction.SECTOR_COMPARISON ->
                "$symbol hissesinin BİST'teki sektör konumu, rakiplerine göre avantaj/dezavantajları ve çarpan dinamiklerini değerlendir."
        }

        val prompt = """
            AKSİYON: ${action.label}
            HİSSE: $symbol (Borsa İstanbul)

            PİYASA VERİSİ:
            $priceContext

            PORTFÖY POZİSYONU:
            $positionContext

            MEVCUT BISTAI ÖZETİ:
            $predContext

            HABER BAŞLIKLARI:
            $newsBlock

            MAKRO BAĞLAM:
            $macroContext

            GÖREV:
            $taskDirective
            Yanıtı Sayın Yatırımcı hitabıyla doğrudan Markdown tablosu, madde işaretleri ve sonunda kısa bir "Risk Özeti" bölümüyle sun. SPK uyarınca yatırım tavsiyesi olmadığını tek satırda belirt.
        """.trimIndent()

        return aiService.generateQuickActionAnalysis(prompt)
    }

    override suspend fun runPortfolioQuickAction(
        action: PortfolioQuickAction,
        portfolioItems: List<PortfolioItem>,
        stocks: List<Stock>,
        newsItems: List<NewsItem>,
        macroContext: String
    ): Result<String> {
        val stockMap = stocks.associateBy { it.symbol }
        val totalInvestment = portfolioItems.sumOf { it.totalInvestment }
        val totalValue = portfolioItems.sumOf { item ->
            val currentPrice = stockMap[item.symbol]?.currentPrice ?: item.averageCost
            currentPrice * item.lotSize
        }
        val netPnl = totalValue - totalInvestment
        val netPnlPct = if (totalInvestment > 0) (netPnl / totalInvestment) * 100.0 else 0.0

        val holdingsBlock = if (portfolioItems.isEmpty()) {
            "Kullanıcı portföyünde henüz kayıtlı hisse bulunmuyor."
        } else {
            portfolioItems.joinToString("\n") { item ->
                val stock = stockMap[item.symbol]
                val price = stock?.currentPrice ?: item.averageCost
                val currentVal = price * item.lotSize
                val weight = if (totalValue > 0) (currentVal / totalValue) * 100.0 else 0.0
                val pnl = currentVal - item.totalInvestment
                val pnlPct = if (item.totalInvestment > 0) (pnl / item.totalInvestment) * 100.0 else 0.0
                val dailyChange = stock?.formattedChangePercent ?: "0.00%"
                "- ${item.symbol}: ${item.lotSize} lot | Maliyet: ${String.format(Locale.US, "%.2f", item.averageCost)} TL | Güncel: ${String.format(Locale.US, "%.2f", price)} TL ($dailyChange) | Değer: ${String.format(Locale.US, "%.2f", currentVal)} TL | Ağırlık: %${String.format(Locale.US, "%.1f", weight)} | K/Z: ${String.format(Locale.US, "%+.2f TL (%+.2f%%)", pnl, pnlPct)}"
            }
        }

        val marketSnapshot = stocks.take(10).joinToString(", ") {
            "${it.symbol}: ${String.format(Locale.US, "%.2f TL", it.currentPrice)} (${it.formattedChangePercent})"
        }.ifBlank { "Piyasa özeti yok" }

        val newsBlock = if (newsItems.isEmpty()) {
            "Güncel ekonomi haberi bulunamadı."
        } else {
            newsItems.joinToString("\n") { "- [${it.source}] ${it.headline}" }
        }

        val taskDirective = when (action) {
            PortfolioQuickAction.DIVERSIFICATION_RISK ->
                "Portföy ağırlık dağılımını, tek hisse/sektör yoğunlaşma riskini ve çeşitlendirme dengesini analiz et. Portföy boşsa BİST risk çeşitlendirme ilkelerini izleme listesi üzerinden özetle."
            PortfolioQuickAction.PROFIT_LOSS_REVIEW ->
                "Portföydeki pozisyonların maliyet, güncel değer ve kâr/zarar performansını kıyasla; taşıma ve zarar kes risklerini değerlendir."
            PortfolioQuickAction.SECTOR_ALLOCATION ->
                "Mevcut portföy hisselerinin BİST sektör dağılımını analiz et; eksik veya yoğun sektörleri tablo ile göster."
            PortfolioQuickAction.STRATEGY_SUMMARY ->
                "Mevcut portföy yapısı ve makro/haber akışı ışığında kısa-orta vadeli portföy takip ve dengeleme stratejisini özetle."
        }

        val prompt = """
            AKSİYON: ${action.label}

            PORTFÖY ÖZETİ:
            - Toplam Maliyet: ${String.format(Locale.US, "%.2f TL", totalInvestment)}
            - Güncel Değer: ${String.format(Locale.US, "%.2f TL", totalValue)}
            - Net Kâr/Zarar: ${String.format(Locale.US, "%+.2f TL (%+.2f%%)", netPnl, netPnlPct)}

            PORTFÖY POZİSYONLARI:
            $holdingsBlock

            BİST PİYASA GÖRÜNÜMÜ:
            $marketSnapshot

            HABER BAŞLIKLARI:
            $newsBlock

            MAKRO BAĞLAM:
            $macroContext

            GÖREV:
            $taskDirective
            Yanıtı Sayın Yatırımcı hitabıyla doğrudan Markdown tablosu, madde işaretleri ve sonunda kısa bir "Risk Özeti" bölümüyle sun. SPK uyarınca yatırım tavsiyesi olmadığını tek satırda belirt.
        """.trimIndent()

        return aiService.generateQuickActionAnalysis(prompt)
    }

    private suspend fun saveToFirebase(symbol: String, prediction: AiPrediction) {
        try {
            val data = mapOf(
                "symbol"          to symbol.uppercase(),
                "trend"           to prediction.trend.name,
                "confidenceScore" to prediction.confidenceScore,
                "targetLow"       to prediction.targetLow,
                "targetHigh"      to prediction.targetHigh,
                "reasoning"       to prediction.reasoning,
                "generatedAt"     to System.currentTimeMillis()
            )
            firestore.collection(COLLECTION)
                .document(symbol.uppercase())
                .set(data)
                .await()
            Log.d(TAG, "✅ Firebase'e yazıldı: $symbol")
        } catch (e: Exception) {
            Log.w(TAG, "Firebase yazma hatası (kritik değil): ${e.message}")
        }
    }

    private fun mapDocToPrediction(data: Map<String, Any>): AiPrediction {
        val trend = com.muzaffer.bistai.domain.model.Trend.valueOf(
            data["trend"] as? String ?: "NEUTRAL"
        )
        return AiPrediction(
            trend           = trend,
            confidenceScore = (data["confidenceScore"] as? Long)?.toInt() ?: 50,
            targetLow       = (data["targetLow"] as? Double) ?: 0.0,
            targetHigh      = (data["targetHigh"] as? Double) ?: 0.0,
            reasoning       = data["reasoning"] as? String ?: "",
            rawAnalysis     = ""
        )
    }
}


package com.muzaffer.bistai.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.muzaffer.bistai.data.local.dao.AnalysisCacheDao
import com.muzaffer.bistai.data.local.entity.AnalysisCacheEntity
import com.muzaffer.bistai.data.remote.AiApiService
import com.muzaffer.bistai.domain.model.AiPrediction
import com.muzaffer.bistai.domain.model.NewsItem
import com.muzaffer.bistai.domain.repository.AnalysisRepository
import com.muzaffer.bistai.domain.repository.AnalysisResult
import com.muzaffer.bistai.domain.repository.AnalysisSource
import kotlinx.coroutines.tasks.await
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


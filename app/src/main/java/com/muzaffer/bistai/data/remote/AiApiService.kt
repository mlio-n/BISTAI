package com.muzaffer.bistai.data.remote

import com.google.ai.client.generativeai.GenerativeModel
import com.muzaffer.bistai.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gemini SDK üzerinden hisse analizi üreten servis.
 */
@Singleton
class AiApiService @Inject constructor() {

    private val model by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey    = BuildConfig.GEMINI_API_KEY
        )
    }

    /**
     * [symbol] hissesi için kısa teknik analiz ve yatırımcı özeti üretir.
     * Hata durumunda null döner.
     */
    suspend fun analyzeStock(symbol: String): Result<String> {
        if (BuildConfig.GEMINI_API_KEY.isBlank()) {
            return Result.failure(IllegalStateException("API_KEY_MISSING"))
        }
        return try {
            val prompt = """
                BIST (Borsa İstanbul) hisselerini analiz eden bir finans uzmanısın.
                
                "$symbol" hissesi için aşağıdaki başlıkları içeren, Türkçe ve kısa (max 250 kelime) bir analiz yaz:
                
                📊 Teknik Görünüm: Kısa vadeli trend, destek/direnç seviyeleri.
                📈 Yatırımcı Özeti: Hem bireysel hem kurumsal yatırımcılar için fırsat/risk değerlendirmesi.
                ⚠️ Dikkat Edilmesi Gerekenler: Önemli riskler veya katalizörler.
                
                Yanıtını net, profesyonel ve aksiyon odaklı yaz. Kesin fiyat tahmini verme.
            """.trimIndent()

            val response = model.generateContent(prompt)
            val text = response.text ?: "Analiz üretilemedi."
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

package com.muzaffer.bistai.data.remote

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.Chat
import com.google.ai.client.generativeai.type.content
import com.muzaffer.bistai.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gemini SDK üzerinden hisse analizi ve sohbet sağlayan servis.
 */
@Singleton
class AiApiService @Inject constructor() {

    private val model by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey    = BuildConfig.GEMINI_API_KEY
        )
    }

    val isApiKeySet: Boolean get() = BuildConfig.GEMINI_API_KEY.isNotBlank()

    /** [symbol] hissesi için kısa teknik analiz ve yatırımcı özeti üretir. */
    suspend fun analyzeStock(symbol: String): Result<String> {
        if (!isApiKeySet) return Result.failure(IllegalStateException("API_KEY_MISSING"))
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
            Result.success(response.text ?: "Analiz üretilemedi.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * [symbol] hissesi bağlamında bir Gemini sohbeti başlatır.
     * Sistem talimatı olarak hisse sembolü enjekte edilir.
     */
    fun startStockChat(symbol: String): Chat {
        val systemContext = content(role = "user") {
            text(
                """
                Sen BISTAI adlı bir finans asistanısın. Kullanıcı seninle "$symbol" 
                (BIST - Borsa İstanbul) hissesi hakkında sohbet edecek. 
                - Türkçe yanıt ver.
                - Finans terminolojisini sade ve anlaşılır kullan.
                - Yatırım tavsiyesi vermekten kaçın, analiz ve bilgi sun.
                - Kısa, odaklı ve net cevaplar ver (max 200 kelime).
                - Her yanıtın sonuna gerekirse mini bir not ekleyebilirsin.
                """.trimIndent()
            )
        }
        val history = listOf(
            systemContext,
            content(role = "model") {
                text("Merhaba! Ben BISTAI finans asistanınım. **$symbol** hissesi hakkında her sorunuzu yanıtlamaya hazırım. Ne merak ediyorsunuz?")
            }
        )
        return model.startChat(history = history)
    }

    /** Mevcut sohbete mesaj gönderir ve yanıtı döner. */
    suspend fun sendMessage(chat: Chat, message: String): Result<String> {
        return try {
            val response = chat.sendMessage(message)
            Result.success(response.text ?: "Yanıt alınamadı.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

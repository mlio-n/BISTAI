package com.muzaffer.bistai.data.remote

import android.util.Log
import com.muzaffer.bistai.data.local.prefs.ApiKeyManager
import com.muzaffer.bistai.domain.model.AiPrediction
import com.muzaffer.bistai.domain.model.NewsItem
import com.muzaffer.bistai.domain.model.Trend
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gemini REST API — v1beta endpoint + gemini-flash-latest model.
 *
 * Endpoint: https://generativelanguage.googleapis.com/v1beta/
 * Auth: X-goog-api-key header
 *
 * BISTAI: Haber + makro bağlam + JSON çıktı formatı ile derinlemesine analiz.
 */
@Singleton
class AiApiService @Inject constructor(
    private val apiKeyManager: ApiKeyManager
) {

    companion object {
        private const val TAG         = "BISTAI_AI"
        private const val BASE_URL    = "https://generativelanguage.googleapis.com/v1beta/models"
        private const val MODEL       = "gemini-flash-latest"
        private const val TEMPERATURE = 0.2
        const val SYSTEM_INSTRUCTION  =
            "Sen sadece Borsa İstanbul (BİST) ve Türkiye piyasaları üzerine uzmanlaşmış analitik bir borsa asistanısın. " +
            "Kullanıcıya daima Sayın Yatırımcı diye hitap et. " +
            "Yanıtlarını asla sohbet uzatmadan, doğrudan Markdown tabloları, madde işaretleri ve kısa risk özetleriyle ver. " +
            "Finans dışı soruları karakterden çıkmadan profesyonelce reddet."
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val isApiKeySet: Boolean get() = apiKeyManager.getEffectiveApiKey().isNotBlank()

    // ─── BISTAI Analizi ──────────────────────────────────────────────────────

    /**
     * Varlık için BISTAI analizi üretir.
     * Haber başlıkları + makro bağlam ile zenginleştirilmiş prompt.
     * Çıktı: Yapılandırılmış JSON → AiPrediction
     */
    suspend fun analyzeAsset(
        symbol: String,
        newsItems: List<NewsItem>,
        macroContext: String,
        currentPrice: Double = 0.0
    ): Result<AiPrediction> {
        if (!isApiKeySet) {
            Log.e(TAG, "❌ API Key boş!")
            return Result.failure(IllegalStateException("API_KEY_MISSING"))
        }

        val newsBlock = newsItems.mapIndexed { i, item ->
            "${i + 1}. [${item.sentiment.name}] ${item.headline} (${item.source})"
        }.joinToString("\n")

        val prompt = """
Sen Borsa İstanbul (BİST) ve Türkiye piyasaları konusunda son derece deneyimli, veriye dayalı çalışan analitik BISTAI borsa asistanısın.
Amacın, sağlanan haberlerin ve fiyatın ardındaki temel ve teknik etkiyi nesnel biçimde analiz etmektir.
Eğer veriler veya haberler yetersizse (veya hiç haber yoksa) varsayım üretme ve "Veri yetersiz, sağlıklı analiz yapılamıyor" şeklinde belirt.

VARLIK: $symbol${if (currentPrice > 0) " | Güncel Fiyat: $currentPrice" else ""}

SON 5 HABER BAŞLIĞI:
${if (newsBlock.isBlank()) "HABER BULUNAMADI" else newsBlock}

MAKRO EKONOMİK DURUM:
$macroContext

GÖREV:
Bu verileri analitik ve nesnel biçimde değerlendir ve YALNIZCA aşağıdaki JSON formatında yanıt ver:
{
  "trend": "BULLISH",
  "confidence": 72,
  "targetLow": 290.0,
  "targetHigh": 320.0,
  "reasoning": "Sayın Yatırımcı için fiyat, haber akışı ve kısa risk özetini içeren 2-3 Türkçe analitik cümle."
}

KURALLAR:
- trend: sadece BULLISH, BEARISH veya NEUTRAL olabilir
- confidence: 0-100 arası sayı (veri yetersizse 0 yap)
- targetLow/targetHigh: gerçekçi fiyat aralığı (0.0 ise bilinmiyor)
- reasoning: Sayın Yatırımcı hitabıyla 2-3 cümle, sadece fiyat/haber ve kısa risk özeti
- SADECE geçerli JSON döndür, kod bloğu veya başka metin ekleme.
        """.trimIndent()

        return try {
            val result = generateContent(
                contents = listOf(userMessage(prompt)),
                expectJson = true
            )
            result.map { rawText -> parseAiPrediction(rawText) }
        } catch (e: Exception) {
            Log.e(TAG, "💥 analyzeAsset hatası: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun generateQuickActionAnalysis(prompt: String): Result<String> {
        if (!isApiKeySet) {
            Log.e(TAG, "❌ API Key boş!")
            return Result.failure(IllegalStateException("API_KEY_MISSING"))
        }
        return generateContent(listOf(userMessage(prompt)))
    }

    /** Geriye dönük uyumluluk için hisse analizi (sadece metin). */
    suspend fun analyzeStock(symbol: String): Result<String> {
        if (!isApiKeySet) return Result.failure(IllegalStateException("API_KEY_MISSING"))
        val prompt = """
            "$symbol" hissesi için BIST piyasası odağında Sayın Yatırımcı'ya yönelik analiz hazırla.
            Markdown tablosu, madde işaretleri ve kısa risk özeti kullan (max 250 kelime):
            | Başlık | Değerlendirme |
        """.trimIndent()
        return generateContent(listOf(userMessage(prompt)))
    }

    // ─── Sohbet Yönetimi ─────────────────────────────────────────────────────

    fun startStockChat(symbol: String, prediction: AiPrediction? = null): SimpleChat {
        val predContext = prediction?.let {
            """
            Mevcut BISTAI Tahmini: ${it.trend.label} (%${it.confidenceScore} güven)
            Hedef Aralık: ${it.targetLow} – ${it.targetHigh}
            """.trimIndent()
        } ?: ""

        val systemPrompt = """
            Odak hisse: "$symbol" (Borsa İstanbul).
            $predContext
            Kurallar:
            1. Sayın Yatırımcı hitabıyla doğrudan konuya gir, gereksiz sohbet uzatma.
            2. Yanıtları Markdown tabloları, madde işaretleri ve kısa bir "Risk Özeti" bölümüyle sun.
            3. Borsa İstanbul, Türkiye ekonomisi ve finans dışındaki soruları karakterden çıkmadan profesyonelce reddet.
            4. Analizlerin SPK kapsamında yatırım tavsiyesi olmadığını kısaca not düş.
        """.trimIndent()

        return SimpleChat(
            initialHistory = mutableListOf(
                userMessage(systemPrompt),
                modelMessage("Sayın Yatırımcı, **$symbol** için analitik veriler hazır. Öğrenmek istediğiniz başlığı paylaşabilirsiniz.")
            )
        )
    }

    fun startGeneralChat(): SimpleChat {
        val systemPrompt = """
            Odak: Borsa İstanbul (BİST) ve Türkiye finansal piyasaları.
            Yanıtları daima Sayın Yatırımcı hitabıyla; doğrudan Markdown tabloları, madde işaretleri ve kısa risk özetleriyle sun.
            Finans ve yatırım dışındaki tüm soruları karakterden çıkmadan profesyonelce reddet.
        """.trimIndent()
        return SimpleChat(
            initialHistory = mutableListOf(
                userMessage(systemPrompt),
                modelMessage("Sayın Yatırımcı, ben **BISTAI**. Borsa İstanbul ve Türkiye piyasalarına dair analiz, sektör kıyaslaması ve risk değerlendirmeleri için hazırım.")
            )
        )
    }

    suspend fun sendMessage(chat: SimpleChat, message: String): Result<String> {
        chat.addUserMessage(message)
        val result = generateContent(chat.history)
        result.onSuccess { reply -> chat.addModelMessage(reply) }
        return result
    }

    // ─── REST Core ───────────────────────────────────────────────────────────

    private suspend fun generateContent(
        contents: List<JSONObject>,
        systemInstruction: String = SYSTEM_INSTRUCTION,
        expectJson: Boolean = false
    ): Result<String> {
        val apiKey = apiKeyManager.getEffectiveApiKey()
        val url = "$BASE_URL/$MODEL:generateContent"
        val body = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
            })
            put("contents", JSONArray(contents))
            put("generationConfig", JSONObject().apply {
                put("maxOutputTokens", 2048)
                put("temperature", TEMPERATURE)
                if (expectJson) {
                    put("responseMimeType", "application/json")
                }
            })
        }

        Log.d(TAG, "📤 URL: $url")
        Log.d(TAG, "📤 Body uzunluğu: ${body.toString().length} karakter")

        return try {
            val request = Request.Builder()
                .url(url)
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .addHeader("X-goog-api-key", apiKey)   // ← Kullanıcının curl'ü ile aynı
                .build()

            val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                client.newCall(request).execute()
            }

            val responseBody = response.body?.string() ?: ""
            Log.d(TAG, "📥 HTTP ${response.code}")
            Log.d(TAG, "📥 Yanıt: $responseBody")

            if (!response.isSuccessful) {
                Log.e(TAG, "❌ Hata! Kod=${response.code} | $responseBody")
                return Result.failure(Exception(parseApiError(response.code, responseBody)))
            }

            val json = JSONObject(responseBody)
            val text = json
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

            Log.d(TAG, "✅ Başarılı (${text.length} karakter)")
            Result.success(text)
        } catch (e: Exception) {
            Log.e(TAG, "💥 İstisna: ${e.javaClass.simpleName}: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ─── AI Yanıt Parser ─────────────────────────────────────────────────────

    private fun parseAiPrediction(rawText: String): AiPrediction {
        return try {
            // JSON bloğunu ayıkla (Gemini bazen ``` ile sarar)
            val jsonStr = rawText
                .replace("```json", "")
                .replace("```", "")
                .trim()
            val json = JSONObject(jsonStr)

            val trend = when (json.optString("trend", "NEUTRAL").uppercase()) {
                "BULLISH" -> Trend.BULLISH
                "BEARISH" -> Trend.BEARISH
                else      -> Trend.NEUTRAL
            }

            AiPrediction(
                trend           = trend,
                confidenceScore = json.optInt("confidence", 50).coerceIn(0, 100),
                targetLow       = json.optDouble("targetLow", 0.0),
                targetHigh      = json.optDouble("targetHigh", 0.0),
                reasoning       = json.optString("reasoning", "Analiz tamamlandı."),
                rawAnalysis     = rawText
            )
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ JSON parse başarısız, ham metin kullanılıyor: ${e.message}")
            AiPrediction(
                trend           = Trend.NEUTRAL,
                confidenceScore = 50,
                targetLow       = 0.0,
                targetHigh      = 0.0,
                reasoning       = rawText.take(300),
                rawAnalysis     = rawText
            )
        }
    }

    private fun parseApiError(code: Int, body: String): String = when {
        code == 400 -> "Geçersiz istek. Analiz için gerekli veriler eksik."
        code == 401 || code == 403 -> "API_KEY_INVALID: Yetkisiz erişim. aistudio.google.com'dan geçerli bir API Key alın."
        code == 404 -> "API_MODEL_NOT_FOUND: Gemini API modeli bulunamadı."
        code == 429 -> "QUOTA_EXCEEDED: Kota aşıldı! Çok fazla analiz istediniz, lütfen 1 dakika bekleyin."
        code >= 500 -> "Google Server Error: Sunucu tarafında hata oluştu ($code)."
        body.contains("timeout") || body.contains("SocketTimeoutException") -> "Bağlantı Sorunu: İnternet yavaş veya kesik."
        else -> "Bilinmeyen API Hatası ($code)"
    }

    // ─── Yardımcı ────────────────────────────────────────────────────────────

    private fun userMessage(text: String) = JSONObject().apply {
        put("role", "user"); put("parts", JSONArray().put(JSONObject().put("text", text)))
    }
    private fun modelMessage(text: String) = JSONObject().apply {
        put("role", "model"); put("parts", JSONArray().put(JSONObject().put("text", text)))
    }
}

/** SDK bağımsız sohbet geçmişi yöneticisi. */
class SimpleChat(val initialHistory: MutableList<JSONObject> = mutableListOf()) {
    val history: List<JSONObject> get() = initialHistory.toList()
    fun addUserMessage(t: String) { initialHistory.add(u(t)) }
    fun addModelMessage(t: String) { initialHistory.add(m(t)) }
    private fun u(t: String) = JSONObject().apply { put("role","user");  put("parts", JSONArray().put(JSONObject().put("text",t))) }
    private fun m(t: String) = JSONObject().apply { put("role","model"); put("parts", JSONArray().put(JSONObject().put("text",t))) }
}

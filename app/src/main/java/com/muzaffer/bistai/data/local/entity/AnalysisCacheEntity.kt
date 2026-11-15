package com.muzaffer.bistai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.muzaffer.bistai.domain.model.AiPrediction
import com.muzaffer.bistai.domain.model.Trend

/**
 * Room entity — Firebase'den veya Gemini API'den gelen analizin yerel yedek kopyası.
 * İnternet yoksa veya Firebase erişilemese bile son başarılı analizi gösterir.
 */
@Entity(tableName = "analysis_cache")
data class AnalysisCacheEntity(
    @PrimaryKey val symbol: String,
    val trend: String,              // Trend.name (BULLISH/BEARISH/NEUTRAL)
    val confidenceScore: Int,
    val targetLow: Double,
    val targetHigh: Double,
    val reasoning: String,
    val rawAnalysis: String,
    val generatedAt: Long,          // epoch ms
    val source: String              // "FIREBASE" | "API"
) {
    fun toAiPrediction() = AiPrediction(
        trend           = Trend.valueOf(trend),
        confidenceScore = confidenceScore,
        targetLow       = targetLow,
        targetHigh      = targetHigh,
        reasoning       = reasoning,
        rawAnalysis     = rawAnalysis
    )

    companion object {
        fun from(symbol: String, prediction: AiPrediction, source: String) =
            AnalysisCacheEntity(
                symbol          = symbol,
                trend           = prediction.trend.name,
                confidenceScore = prediction.confidenceScore,
                targetLow       = prediction.targetLow,
                targetHigh      = prediction.targetHigh,
                reasoning       = prediction.reasoning,
                rawAnalysis     = prediction.rawAnalysis,
                generatedAt     = System.currentTimeMillis(),
                source          = source
            )
    }
}

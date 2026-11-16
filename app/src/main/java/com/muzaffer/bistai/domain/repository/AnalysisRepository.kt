package com.muzaffer.bistai.domain.repository

import com.muzaffer.bistai.domain.model.AiPrediction
import com.muzaffer.bistai.domain.model.NewsItem
import com.muzaffer.bistai.domain.model.PortfolioItem
import com.muzaffer.bistai.domain.model.PortfolioQuickAction
import com.muzaffer.bistai.domain.model.Stock
import com.muzaffer.bistai.domain.model.StockQuickAction

enum class AnalysisSource { FIREBASE, LOCAL_CACHE, FRESH_API }

data class AnalysisResult(
    val prediction: AiPrediction,
    val source: AnalysisSource,
    val generatedAt: Long               // epoch ms — kaç dakika önce üretildi
)

/**
 * Analiz havuzu soyutlaması.
 * Uygulama: Firebase → Gemini API → Room cache öncelik sırası.
 */
interface AnalysisRepository {
    suspend fun getAnalysis(
        symbol: String,
        newsItems: List<NewsItem>,
        macroContext: String
    ): Result<AnalysisResult>

    suspend fun runStockQuickAction(
        action: StockQuickAction,
        symbol: String,
        stock: Stock?,
        portfolioItem: PortfolioItem?,
        newsItems: List<NewsItem>,
        macroContext: String,
        prediction: AiPrediction?
    ): Result<String>

    suspend fun runPortfolioQuickAction(
        action: PortfolioQuickAction,
        portfolioItems: List<PortfolioItem>,
        stocks: List<Stock>,
        newsItems: List<NewsItem>,
        macroContext: String
    ): Result<String>
}


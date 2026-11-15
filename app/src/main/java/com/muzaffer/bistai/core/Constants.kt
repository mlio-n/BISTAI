package com.muzaffer.bistai.core

object Constants {
    // API Base URLs
    const val BASE_URL = "https://api.bistai.com/v1/"
    const val YF_BASE_URL = "https://query1.finance.yahoo.com/"
    const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    const val GEMINI_MODEL = "gemini-flash-latest"
    const val TRT_RSS_URL = "https://www.trthaber.com/ekonomi_articles.rss"

    // Room Database
    const val DATABASE_NAME = "bistai_database"

    // Firestore Collections & Cache TTL
    const val ANALYSES_COLLECTION = "analyses"
    const val USERS_COLLECTION = "users"
    const val PORTFOLIO_COLLECTION = "portfolio"
    const val ANALYSIS_TTL_MS = 24 * 60 * 60 * 1000L // 24 saat

    // Network Timeouts
    const val NETWORK_TIMEOUT_SECONDS = 30L
}

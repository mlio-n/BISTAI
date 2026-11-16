package com.muzaffer.bistai.core

object Constants {
    const val BASE_URL = "https://api.bistai.com/v1/"
    const val YF_BASE_URL = "https://query1.finance.yahoo.com/"
    const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    const val GEMINI_MODEL = "gemini-flash-latest"
    const val TRT_RSS_URL = "https://www.trthaber.com/ekonomi_articles.rss"

    const val DATABASE_NAME = "bistai_database"

    const val ANALYSES_COLLECTION = "analyses"
    const val USERS_COLLECTION = "bistai_users"
    const val PORTFOLIO_COLLECTION = "portfolio"
    const val ANALYSIS_TTL_MS = 24 * 60 * 60 * 1000L

    const val NETWORK_TIMEOUT_SECONDS = 30L
}

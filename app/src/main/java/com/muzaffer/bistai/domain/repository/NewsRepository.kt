package com.muzaffer.bistai.domain.repository

import com.muzaffer.bistai.domain.model.NewsItem

/**
 * Haber kaynağı soyutlaması.
 * Gerçek GNews API + FakeNewsDataSource (yedek) implementasyonu mevcuttur.
 */
interface NewsRepository {
    /** Verilen sembol için haber başlıklarını döner (ağ çağrısı içerebilir). */
    suspend fun getNewsForAsset(symbol: String): List<NewsItem>
    fun getMacroContext(): String
}

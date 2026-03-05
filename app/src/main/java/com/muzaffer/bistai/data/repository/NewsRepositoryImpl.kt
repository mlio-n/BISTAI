package com.muzaffer.bistai.data.repository

import android.util.Log
import com.muzaffer.bistai.data.local.fake.FakeNewsDataSource
import com.muzaffer.bistai.data.remote.RealNewsDataSource
import com.muzaffer.bistai.domain.model.NewsItem
import com.muzaffer.bistai.domain.repository.NewsRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [NewsRepository] interface'inin somut implementasyonu.
 *
 * Önce [RealNewsDataSource] (GNews API) denenir.
 * API anahtarı yoksa veya çağrı başarısız olursa [FakeNewsDataSource]'a düşülür.
 */
@Singleton
class NewsRepositoryImpl @Inject constructor(
    private val realNewsDataSource: RealNewsDataSource,
    private val fakeNewsDataSource: FakeNewsDataSource
) : NewsRepository {

    override suspend fun getNewsForAsset(symbol: String): List<NewsItem> {
        val realNews = realNewsDataSource.getNewsForAsset(symbol)
        if (realNews.isNotEmpty()) return realNews

        // Gerçek API sonuç vermezse sahte veriye dön
        Log.d("NewsRepo", "Gerçek haber bulunamadı, sahte veri kullanılıyor: $symbol")
        return fakeNewsDataSource.getNewsForAsset(symbol)
    }

    override fun getMacroContext(): String =
        fakeNewsDataSource.getMacroContext()
}

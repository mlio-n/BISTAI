package com.muzaffer.bistai.data.remote

import com.muzaffer.bistai.domain.model.NewsItem
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google News RSS feed'ini kullanarak gerçek Türkçe haber başlıkları döndüren veri kaynağı.
 *
 * API key gerektirmez — tamamen ücretsiz ve sınırsız.
 * Tüm işlevsellik [GoogleNewsRssDataSource]'a delege edilir.
 * Hata durumunda boş liste döner; çağıran katman ([NewsRepositoryImpl]) fake veriye düşer.
 */
@Singleton
class RealNewsDataSource @Inject constructor(
    private val googleNewsRssDataSource: GoogleNewsRssDataSource
) {
    /** Google News RSS API key gerektirmez; her zaman true döner. */
    val isApiKeySet: Boolean = true

    /**
     * Verilen sembol için en fazla 5 gerçek haber başlığı döner.
     * Hata oluşursa boş liste döner.
     */
    suspend fun getNewsForAsset(symbol: String): List<NewsItem> =
        googleNewsRssDataSource.getNewsForAsset(symbol)
}

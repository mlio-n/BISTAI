package com.muzaffer.bistai.domain.repository

import com.muzaffer.bistai.domain.model.PortfolioItem
import kotlinx.coroutines.flow.Flow

interface PortfolioRepository {
    /**
     * Kullanıcının portföyündeki tüm hisseleri yerel Room veritabanından (veya giriş yapıldıysa bulutla senkron) dinler.
     */
    fun getPortfolio(uid: String? = null): Flow<List<PortfolioItem>>

    /**
     * Portföye yeni hisse ekler veya günceller. Misafir modunda yalnızca yerel Room veritabanına yazar.
     */
    suspend fun addOrUpdatePortfolioItem(uid: String? = null, item: PortfolioItem): Result<Unit>

    /**
     * Hisseyi portföyden çıkarır. Misafir modunda yalnızca yerel Room veritabanından siler.
     */
    suspend fun removePortfolioItem(uid: String? = null, symbol: String): Result<Unit>

    /**
     * Yerel Room veritabanındaki misafir portföy verilerini kullanıcının Firestore hesabına yedekler.
     */
    suspend fun syncLocalToCloud(uid: String): Result<Unit>
}

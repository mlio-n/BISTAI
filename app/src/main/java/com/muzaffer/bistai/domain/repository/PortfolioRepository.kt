package com.muzaffer.bistai.domain.repository

import com.muzaffer.bistai.domain.model.PortfolioItem
import kotlinx.coroutines.flow.Flow

interface PortfolioRepository {
    fun getPortfolio(uid: String? = null): Flow<List<PortfolioItem>>
    suspend fun addOrUpdatePortfolioItem(uid: String? = null, item: PortfolioItem): Result<Unit>
    suspend fun removePortfolioItem(uid: String? = null, symbol: String): Result<Unit>
    suspend fun syncLocalToCloud(uid: String): Result<Unit>
}

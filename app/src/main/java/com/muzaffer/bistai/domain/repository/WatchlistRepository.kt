package com.muzaffer.bistai.domain.repository

import com.muzaffer.bistai.data.local.entity.StockEntity
import kotlinx.coroutines.flow.Flow

/**
 * Watchlist (favori hisseler) işlemleri için domain sözleşmesi.
 */
interface WatchlistRepository {
    fun getAllFavorites(): Flow<List<StockEntity>>
    fun isFavorite(symbol: String): Flow<Boolean>
    suspend fun addFavorite(symbol: String, name: String)
    suspend fun removeFavorite(symbol: String)
    suspend fun toggleFavorite(symbol: String, name: String)
}

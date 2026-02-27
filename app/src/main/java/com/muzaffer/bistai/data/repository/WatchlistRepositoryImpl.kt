package com.muzaffer.bistai.data.repository

import com.muzaffer.bistai.data.local.dao.StockDao
import com.muzaffer.bistai.data.local.entity.StockEntity
import com.muzaffer.bistai.domain.repository.WatchlistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchlistRepositoryImpl @Inject constructor(
    private val dao: StockDao
) : WatchlistRepository {

    override fun getAllFavorites(): Flow<List<StockEntity>> =
        dao.getAllFavorites()

    override fun isFavorite(symbol: String): Flow<Boolean> =
        dao.isFavorite(symbol)

    override suspend fun addFavorite(symbol: String, name: String) {
        dao.addFavorite(StockEntity(symbol = symbol, name = name))
    }

    override suspend fun removeFavorite(symbol: String) {
        dao.removeFavorite(symbol)
    }

    override suspend fun toggleFavorite(symbol: String, name: String) {
        if (dao.isFavorite(symbol).first()) {
            dao.removeFavorite(symbol)
        } else {
            dao.addFavorite(StockEntity(symbol = symbol, name = name))
        }
    }
}

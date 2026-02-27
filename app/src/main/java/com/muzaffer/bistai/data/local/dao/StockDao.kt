package com.muzaffer.bistai.data.local.dao

import androidx.room.*
import com.muzaffer.bistai.data.local.entity.StockEntity
import kotlinx.coroutines.flow.Flow

/**
 * Watchlist (favori hisseler) için CRUD operasyonları.
 */
@Dao
interface StockDao {

    /** Tüm favorileri eklenme tarihine göre sıralı döner (Flow — anlık güncellenir). */
    @Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<StockEntity>>

    /** Sembolün favoride olup olmadığını döner. */
    @Query("SELECT COUNT(*) > 0 FROM watchlist WHERE symbol = :symbol")
    fun isFavorite(symbol: String): Flow<Boolean>

    /** Favori ekle. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(stock: StockEntity)

    /** Favori sil. */
    @Query("DELETE FROM watchlist WHERE symbol = :symbol")
    suspend fun removeFavorite(symbol: String)
}

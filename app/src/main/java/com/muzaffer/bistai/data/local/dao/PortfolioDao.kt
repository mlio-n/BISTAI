package com.muzaffer.bistai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.muzaffer.bistai.data.local.entity.PortfolioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PortfolioDao {
    @Query("SELECT * FROM portfolio ORDER BY addedAt DESC")
    fun getAllPortfolioItems(): Flow<List<PortfolioEntity>>

    @Query("SELECT * FROM portfolio")
    suspend fun getAllPortfolioItemsOnce(): List<PortfolioEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPortfolioItem(item: PortfolioEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<PortfolioEntity>)

    @Query("DELETE FROM portfolio WHERE symbol = :symbol")
    suspend fun deletePortfolioItem(symbol: String)

    @Query("DELETE FROM portfolio WHERE symbol NOT IN (:symbols)")
    suspend fun deleteItemsNotIn(symbols: List<String>)

    @Query("DELETE FROM portfolio")
    suspend fun clearAll()
}

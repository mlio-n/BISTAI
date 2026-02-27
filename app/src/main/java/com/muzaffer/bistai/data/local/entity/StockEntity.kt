package com.muzaffer.bistai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Favori listesindeki hisseleri tutan Room entity'si.
 */
@Entity(tableName = "watchlist")
data class StockEntity(
    @PrimaryKey
    val symbol: String,
    val name: String,
    val addedAt: Long = System.currentTimeMillis()
)

package com.muzaffer.bistai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.muzaffer.bistai.domain.model.PortfolioItem

@Entity(tableName = "portfolio")
data class PortfolioEntity(
    @PrimaryKey val symbol: String,
    val lotSize: Double,
    val averageCost: Double,
    val addedAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = PortfolioItem(
        symbol = symbol,
        lotSize = lotSize,
        averageCost = averageCost,
        addedAt = addedAt
    )

    companion object {
        fun fromDomain(item: PortfolioItem) = PortfolioEntity(
            symbol = item.symbol,
            lotSize = item.lotSize,
            averageCost = item.averageCost,
            addedAt = item.addedAt
        )
    }
}

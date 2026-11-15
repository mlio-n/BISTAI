package com.muzaffer.bistai.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.muzaffer.bistai.data.local.dao.AnalysisCacheDao
import com.muzaffer.bistai.data.local.entity.AnalysisCacheEntity
import com.muzaffer.bistai.data.local.dao.PortfolioDao
import com.muzaffer.bistai.data.local.dao.StockDao
import com.muzaffer.bistai.data.local.entity.PortfolioEntity
import com.muzaffer.bistai.data.local.entity.StockEntity

@Database(
    entities = [StockEntity::class, AnalysisCacheEntity::class, PortfolioEntity::class],
    version = 3,
    exportSchema = false
)
abstract class BistaiDatabase : RoomDatabase() {
    abstract fun stockDao(): StockDao
    abstract fun analysisCacheDao(): AnalysisCacheDao
    abstract fun portfolioDao(): PortfolioDao
}

package com.muzaffer.bistai.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.muzaffer.bistai.data.local.dao.AnalysisCacheDao
import com.muzaffer.bistai.data.local.entity.AnalysisCacheEntity
import com.muzaffer.bistai.data.local.dao.StockDao
import com.muzaffer.bistai.data.local.entity.StockEntity

@Database(
    entities = [StockEntity::class, AnalysisCacheEntity::class],
    version = 2,
    exportSchema = false
)
abstract class BistaiDatabase : RoomDatabase() {
    abstract fun stockDao(): StockDao
    abstract fun analysisCacheDao(): AnalysisCacheDao
}

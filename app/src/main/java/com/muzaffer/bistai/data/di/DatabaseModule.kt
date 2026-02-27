package com.muzaffer.bistai.data.di

import android.content.Context
import androidx.room.Room
import com.muzaffer.bistai.data.local.dao.StockDao
import com.muzaffer.bistai.data.local.database.BistaiDatabase
import com.muzaffer.bistai.data.repository.WatchlistRepositoryImpl
import com.muzaffer.bistai.domain.repository.WatchlistRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BistaiDatabase =
        Room.databaseBuilder(
            context,
            BistaiDatabase::class.java,
            "bistai_database"
        ).fallbackToDestructiveMigration().build()

    @Provides
    @Singleton
    fun provideStockDao(db: BistaiDatabase): StockDao = db.stockDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class WatchlistModule {

    @Binds
    @Singleton
    abstract fun bindWatchlistRepository(
        impl: WatchlistRepositoryImpl
    ): WatchlistRepository
}

package com.muzaffer.bistai.data.di

import com.muzaffer.bistai.data.repository.StockRepositoryImpl
import com.muzaffer.bistai.domain.repository.StockRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt modülü — domain katmanındaki abstraction'ı
 * data katmanındaki somut implementasyona bağlar.
 *
 * [Binds] kullanıldığı için sınıf abstract olmalıdır.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindStockRepository(
        impl: StockRepositoryImpl
    ): StockRepository
}

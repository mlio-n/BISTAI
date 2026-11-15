package com.muzaffer.bistai.di

import com.muzaffer.bistai.data.repository.AnalysisRepositoryImpl
import com.muzaffer.bistai.data.repository.StockRepositoryImpl
import com.muzaffer.bistai.domain.repository.AnalysisRepository
import com.muzaffer.bistai.domain.repository.StockRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt modülü — domain katmanındaki abstraction'ları
 * data katmanındaki somut implementasyonlara bağlar.
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

    @Binds
    @Singleton
    abstract fun bindAnalysisRepository(
        impl: AnalysisRepositoryImpl
    ): AnalysisRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: com.muzaffer.bistai.data.repository.AuthRepositoryImpl
    ): com.muzaffer.bistai.domain.repository.AuthRepository

    @Binds
    @Singleton
    abstract fun bindPortfolioRepository(
        impl: com.muzaffer.bistai.data.repository.PortfolioRepositoryImpl
    ): com.muzaffer.bistai.domain.repository.PortfolioRepository
}

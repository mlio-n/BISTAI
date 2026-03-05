package com.muzaffer.bistai.data.di

import com.muzaffer.bistai.data.remote.StockApiService
import com.muzaffer.bistai.data.remote.YahooFinanceApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /** Orijinal BISTAI API (ileride kullanılmak üzere korunuyor). */
    private const val BISTAI_BASE_URL       = "https://api.bistai.com/v1/"

    /** Yahoo Finance v7 endpoint'i — BIST + kripto + döviz fiyatları için. */
    private const val YAHOO_FINANCE_BASE_URL = "https://query1.finance.yahoo.com/"

    // ─── Ortak OkHttpClient ─────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Accept", "application/json")
                    // User-Agent tüm API çağrılarına eklenir;
                    // Yahoo Finance bazı durumlarda bu header'ı gerektirir.
                    .addHeader("User-Agent", "Mozilla/5.0 (compatible; BISTAI/1.0)")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

    // ─── Orijinal BISTAI API (gelecek kullanım için) ────────────────────────

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BISTAI_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideStockApiService(retrofit: Retrofit): StockApiService =
        retrofit.create(StockApiService::class.java)

    // ─── Yahoo Finance ──────────────────────────────────────────────────────

    @Provides
    @Singleton
    @YahooFinanceClient
    fun provideYahooFinanceRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(YAHOO_FINANCE_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideYahooFinanceApiService(
        @YahooFinanceClient retrofit: Retrofit
    ): YahooFinanceApiService = retrofit.create(YahooFinanceApiService::class.java)
}

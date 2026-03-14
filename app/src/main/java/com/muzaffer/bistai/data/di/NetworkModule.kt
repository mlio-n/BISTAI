package com.muzaffer.bistai.data.di

import com.muzaffer.bistai.BuildConfig
import com.muzaffer.bistai.data.remote.StockApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/** Yahoo Finance için özel Qualifier — Hilt'e hangi OkHttpClient'i vereceğini söyler */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class YfHttpClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://api.bistai.com/v1/"
    private const val YF_BASE_URL = "https://query1.finance.yahoo.com/"

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
        }

    /** Genel API istemcisi */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .addHeader("Accept", "application/json")
                        .build()
                )
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

    /**
     * Yahoo Finance için özel OkHttpClient.
     * Yahoo Finance, browser olmayan User-Agent'ları bloklıyor.
     */
    @Provides
    @Singleton
    @YfHttpClient
    fun provideYfOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .addHeader(
                            "User-Agent",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) " +
                            "Chrome/120.0.0.0 Safari/537.36"
                        )
                        .addHeader("Referer", "https://finance.yahoo.com")
                        .addHeader("Accept", "application/json")
                        .addHeader("Accept-Language", "en-US,en;q=0.9")
                        .build()
                )
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideStockApiService(retrofit: Retrofit): StockApiService =
        retrofit.create(StockApiService::class.java)

    @Provides
    @Singleton
    fun provideYfApiService(
        @YfHttpClient yfClient: OkHttpClient
    ): com.muzaffer.bistai.data.remote.YfApiService =
        Retrofit.Builder()
            .baseUrl(YF_BASE_URL)
            .client(yfClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(com.muzaffer.bistai.data.remote.YfApiService::class.java)
}

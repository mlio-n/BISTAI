package com.muzaffer.bistai.data.remote

import com.muzaffer.bistai.data.remote.dto.GNewsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * GNews API için Retrofit arayüzü.
 *
 * Base URL: https://gnews.io/api/v4/
 * Ücretsiz katman: günde 100 istek, maks. 10 makale/istek.
 * API anahtarı: gnews.io adresinden ücretsiz alınır.
 */
interface GNewsApiService {

    /**
     * Verilen sorguyla eşleşen haber başlıklarını döner.
     * @param query   Arama sorgusu (örn. "Türk Hava Yolları THY")
     * @param lang    Dil kodu (varsayılan: "tr")
     * @param max     Maksimum makale sayısı (varsayılan: 5, maks: 10)
     * @param apiKey  GNews API anahtarı
     */
    @GET("search")
    suspend fun searchNews(
        @Query("q")      query: String,
        @Query("lang")   lang: String = "tr",
        @Query("max")    max: Int = 5,
        @Query("apikey") apiKey: String
    ): Response<GNewsResponse>
}

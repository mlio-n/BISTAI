package com.muzaffer.bistai.data.remote

import com.muzaffer.bistai.data.remote.dto.StockDto
import com.muzaffer.bistai.data.remote.dto.StockListResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface — gerçek borsa REST API endpoint'lerini tanımlar.
 *
 * Örnek base URL: https://api.bistai.com/v1/
 */
interface StockApiService {

    /** Tüm hisseler listesini getirir (sayfalama destekler). */
    @GET("stocks")
    suspend fun getStocks(
        @Query("page")  page: Int = 1,
        @Query("limit") limit: Int = 50
    ): Response<StockListResponse>

    /** Tek bir hissenin detayını sembolüne göre getirir. */
    @GET("stocks/{symbol}")
    suspend fun getStockDetail(
        @Path("symbol") symbol: String
    ): Response<StockDto>

    /** Portföydeki hisseleri tek sorguda getirir (virgülle ayrılmış semboller). */
    @GET("stocks/batch")
    suspend fun getBatchStocks(
        @Query("symbols") symbols: String
    ): Response<StockListResponse>
}

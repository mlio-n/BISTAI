package com.muzaffer.bistai.data.remote

import com.muzaffer.bistai.data.remote.dto.yf.YfChartResponse
import com.muzaffer.bistai.data.remote.dto.yf.YfResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface YfApiService {

    @GET("v7/finance/quote")
    suspend fun getQuotes(
        @Query("symbols") symbols: String,
        @Query("region") region: String = "US",
        @Query("lang") lang: String = "en"
    ): YfResponse

    @GET("v8/finance/chart/{symbol}")
    suspend fun getChartQuote(
        @Path("symbol") symbol: String,
        @Query("interval") interval: String = "1d",
        @Query("range") range: String = "1d"
    ): YfChartResponse
}

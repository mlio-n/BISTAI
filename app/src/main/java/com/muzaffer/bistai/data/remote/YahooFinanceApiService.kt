package com.muzaffer.bistai.data.remote

import com.muzaffer.bistai.data.remote.dto.YahooFinanceResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Yahoo Finance v7 /quote endpoint'i için Retrofit arayüzü.
 *
 * Base URL: https://query1.finance.yahoo.com/
 * API anahtarı gerektirmez; ücretsiz, açık bir endpoint'tir.
 *
 * BIST hisseleri için sembol sonuna ".IS" eklenir (örn. THYAO → THYAO.IS).
 */
interface YahooFinanceApiService {

    /**
     * Verilen semboller için anlık fiyat verisi döner.
     * @param symbols Virgülle ayrılmış Yahoo Finance sembol listesi
     *                (örn. "THYAO.IS,GARAN.IS,BTC-USD,USDTRY=X")
     */
    @GET("v7/finance/quote")
    suspend fun getQuotes(
        @Query("symbols") symbols: String
    ): Response<YahooFinanceResponse>
}

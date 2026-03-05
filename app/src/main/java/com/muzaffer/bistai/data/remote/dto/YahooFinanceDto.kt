package com.muzaffer.bistai.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Yahoo Finance v7 /quote endpoint'inin kök yanıt modeli. */
data class YahooFinanceResponse(
    @SerializedName("quoteResponse") val quoteResponse: YahooQuoteResponse
)

data class YahooQuoteResponse(
    @SerializedName("result") val result: List<YahooQuoteResult>?,
    @SerializedName("error")  val error: Any?
)

data class YahooQuoteResult(
    @SerializedName("symbol")                     val symbol: String,
    @SerializedName("shortName")                  val shortName: String?,
    @SerializedName("longName")                   val longName: String?,
    @SerializedName("regularMarketPrice")         val regularMarketPrice: Double?,
    @SerializedName("regularMarketChangePercent") val regularMarketChangePercent: Double?,
    @SerializedName("regularMarketPreviousClose") val regularMarketPreviousClose: Double?,
    @SerializedName("marketCap")                  val marketCap: Long?,
    @SerializedName("regularMarketVolume")        val regularMarketVolume: Long?
)

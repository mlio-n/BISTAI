package com.muzaffer.bistai.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Gerçek bir borsa REST API'sinin döneceği tipik JSON modelini temsil eder.
 * Örnek: https://api.example.com/v1/stocks
 */
data class StockListResponse(
    @SerializedName("stocks") val stocks: List<StockDto>
)

data class StockDto(
    @SerializedName("symbol")         val symbol: String,
    @SerializedName("name")           val name: String,
    @SerializedName("current_price")  val currentPrice: Double,
    @SerializedName("change_percent") val changePercent: Double,
    @SerializedName("previous_close") val previousClose: Double?,
    @SerializedName("market_cap")     val marketCap: Long?,
    @SerializedName("volume")         val volume: Long?
)

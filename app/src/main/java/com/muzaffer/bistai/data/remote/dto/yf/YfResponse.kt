package com.muzaffer.bistai.data.remote.dto.yf

import com.google.gson.annotations.SerializedName

data class YfResponse(
    val quoteResponse: QuoteResponse
)

data class QuoteResponse(
    val result: List<YfQuoteDto>,
    val error: Any?
)

data class YfQuoteDto(
    val symbol: String,
    val shortName: String?,
    val longName: String?,
    val regularMarketPrice: Double?,
    val regularMarketChangePercent: Double?,
    val regularMarketPreviousClose: Double?,
    val marketCap: Long?,
    val regularMarketVolume: Long?
)

data class YfChartResponse(
    val chart: YfChart?
)

data class YfChart(
    val result: List<YfChartResult>?,
    val error: Any?
)

data class YfChartResult(
    val meta: YfChartMeta?
)

data class YfChartMeta(
    val symbol: String?,
    val shortName: String?,
    val longName: String?,
    val regularMarketPrice: Double?,
    val regularMarketChangePercent: Double?,
    val chartPreviousClose: Double?,
    val previousClose: Double?,
    val regularMarketVolume: Long?
)

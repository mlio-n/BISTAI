package com.muzaffer.bistai.domain.model

import com.muzaffer.bistai.core.formatTrPercent

data class Stock(
    val symbol: String,
    val name: String,
    val currentPrice: Double,
    val changePercent: Double,
    val previousClose: Double? = null,
    val marketCap: Long? = null,
    val volume: Long? = null
) {
    val changeAmount: Double
        get() = previousClose?.let { currentPrice - it } ?: 0.0

    val isBullish: Boolean
        get() = changePercent >= 0.0

    val formattedChangePercent: String
        get() = changePercent.formatTrPercent()
}

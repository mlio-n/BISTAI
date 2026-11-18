package com.muzaffer.bistai.domain.model

enum class ChartTimeRange(
    val label: String,
    val apiRange: String,
    val apiInterval: String
) {
    ONE_DAY("1G", "1d", "5m"),
    ONE_WEEK("1H", "5d", "15m"),
    ONE_MONTH("1A", "1mo", "1d"),
    THREE_MONTHS("3A", "3mo", "1d"),
    ONE_YEAR("1Y", "1y", "1d")
}

data class ChartPoint(
    val timestamp: Long,
    val closePrice: Double
)

data class ChartData(
    val symbol: String,
    val range: ChartTimeRange,
    val points: List<ChartPoint>,
    val currentPrice: Double,
    val previousClose: Double?
) {
    val firstPrice: Double
        get() = points.firstOrNull()?.closePrice ?: previousClose ?: currentPrice

    val lastPrice: Double
        get() = points.lastOrNull()?.closePrice ?: currentPrice

    val minPrice: Double
        get() = points.minOfOrNull { it.closePrice } ?: 0.0

    val maxPrice: Double
        get() = points.maxOfOrNull { it.closePrice } ?: 0.0

    val priceChange: Double
        get() = lastPrice - firstPrice

    val priceChangePercent: Double
        get() = if (firstPrice > 0.0) (priceChange / firstPrice) * 100.0 else 0.0

    val isPositive: Boolean
        get() = lastPrice >= firstPrice
}

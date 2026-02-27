package com.muzaffer.bistai.data.mapper

import com.muzaffer.bistai.data.remote.dto.StockDto
import com.muzaffer.bistai.domain.model.Stock

/**
 * DTO → Domain model dönüşüm katmanı.
 * Data katmanını domain katmanından izole eder.
 */
fun StockDto.toDomain(): Stock = Stock(
    symbol        = symbol,
    name          = name,
    currentPrice  = currentPrice,
    changePercent = changePercent,
    previousClose = previousClose,
    marketCap     = marketCap,
    volume        = volume
)

fun List<StockDto>.toDomain(): List<Stock> = map { it.toDomain() }

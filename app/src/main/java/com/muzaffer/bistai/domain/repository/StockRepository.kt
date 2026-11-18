package com.muzaffer.bistai.domain.repository

import com.muzaffer.bistai.core.Resource
import com.muzaffer.bistai.domain.model.ChartData
import com.muzaffer.bistai.domain.model.ChartTimeRange
import com.muzaffer.bistai.domain.model.Stock
import kotlinx.coroutines.flow.Flow

interface StockRepository {
    fun getStocks(): Flow<Resource<List<Stock>>>
    fun getStockDetail(symbol: String): Flow<Resource<Stock>>
    fun getBatchStocks(symbols: List<String>): Flow<Resource<List<Stock>>>
    fun getStockChart(symbol: String, range: ChartTimeRange): Flow<Resource<ChartData>>
}

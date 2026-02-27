package com.muzaffer.bistai.domain.repository

import com.muzaffer.bistai.domain.model.Stock
import com.muzaffer.bistai.domain.util.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Domain katmanının veri kaynağından bağımsız sözleşmesi.
 * Data katmanındaki implementasyon bu interface'i doldurur.
 */
interface StockRepository {

    /** Borsa hisse listesini döner (Flow içinde Loading → Success/Error). */
    fun getStocks(): Flow<Resource<List<Stock>>>

    /** Sembol bazında tek hisse detayını döner. */
    fun getStockDetail(symbol: String): Flow<Resource<Stock>>

    /** Portföydeki hisseleri toplu olarak çeker. */
    fun getBatchStocks(symbols: List<String>): Flow<Resource<List<Stock>>>
}

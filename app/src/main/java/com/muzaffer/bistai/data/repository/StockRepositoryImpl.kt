package com.muzaffer.bistai.data.repository

import android.util.Log
import com.muzaffer.bistai.data.local.fake.FakeStockDataSource
import com.muzaffer.bistai.data.mapper.toDomain
import com.muzaffer.bistai.data.remote.RealStockDataSource
import com.muzaffer.bistai.data.remote.StockApiService
import com.muzaffer.bistai.domain.model.Stock
import com.muzaffer.bistai.domain.repository.StockRepository
import com.muzaffer.bistai.domain.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * [StockRepository] interface'inin somut implementasyonu.
 *
 * Önce [RealStockDataSource] (Yahoo Finance API) denenir.
 * API başarısız olursa [FakeStockDataSource]'a düşülür.
 */
class StockRepositoryImpl @Inject constructor(
    private val apiService: StockApiService,
    private val realStockDataSource: RealStockDataSource,
    private val fakeDataSource: FakeStockDataSource
) : StockRepository {

    override fun getStocks(): Flow<Resource<List<Stock>>> = flow {
        emit(Resource.Loading)
        try {
            val dtos = realStockDataSource.getStocks()
            if (dtos.isNotEmpty()) {
                emit(Resource.Success(dtos.toDomain()))
                return@flow
            }
        } catch (e: Exception) {
            Log.w("StockRepo", "Yahoo Finance başarısız, sahte veriye düşülüyor: ${e.message}")
        }
        // Yedek: sahte veri kaynağı
        try {
            emit(Resource.Success(fakeDataSource.getStocks().toDomain()))
        } catch (e: Exception) {
            emit(Resource.Error(message = e.localizedMessage ?: "Bilinmeyen hata", throwable = e))
        }
    }

    override fun getStockDetail(symbol: String): Flow<Resource<Stock>> = flow {
        emit(Resource.Loading)
        try {
            val dto = realStockDataSource.getStockDetail(symbol)
            if (dto != null) {
                emit(Resource.Success(dto.toDomain()))
                return@flow
            }
        } catch (e: Exception) {
            Log.w("StockRepo", "Yahoo Finance detay başarısız: ${e.message}")
        }
        try {
            val dto = fakeDataSource.getStockDetail(symbol)
                ?: return@flow emit(Resource.Error("$symbol bulunamadı"))
            emit(Resource.Success(dto.toDomain()))
        } catch (e: Exception) {
            emit(Resource.Error(message = e.localizedMessage ?: "Bilinmeyen hata", throwable = e))
        }
    }

    override fun getBatchStocks(symbols: List<String>): Flow<Resource<List<Stock>>> = flow {
        emit(Resource.Loading)
        try {
            val dtos = realStockDataSource.getBatchStocks(symbols)
            if (dtos.isNotEmpty()) {
                emit(Resource.Success(dtos.toDomain()))
                return@flow
            }
        } catch (e: Exception) {
            Log.w("StockRepo", "Yahoo Finance batch başarısız: ${e.message}")
        }
        try {
            emit(Resource.Success(fakeDataSource.getBatchStocks(symbols).toDomain()))
        } catch (e: Exception) {
            emit(Resource.Error(message = e.localizedMessage ?: "Bilinmeyen hata", throwable = e))
        }
    }
}

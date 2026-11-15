package com.muzaffer.bistai.data.repository

import com.muzaffer.bistai.data.remote.RemoteStockDataSource
import com.muzaffer.bistai.domain.model.Stock
import com.muzaffer.bistai.domain.repository.StockRepository
import com.muzaffer.bistai.core.Resource
import com.muzaffer.bistai.data.mapper.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * [StockRepository] interface'inin somut implementasyonu.
 * Artık Fake veriyi değil, Yahoo Finance kullanan RemoteStockDataSource'u baz alır.
 */
class StockRepositoryImpl @Inject constructor(
    private val remoteDataSource: RemoteStockDataSource
) : StockRepository {

    override fun getStocks(): Flow<Resource<List<Stock>>> = flow {
        emit(Resource.Loading)
        try {
            val dtos = remoteDataSource.getStocks()
            emit(Resource.Success(dtos.toDomain()))
        } catch (e: Exception) {
            emit(Resource.Error(message = e.localizedMessage ?: "Bilinmeyen hata", throwable = e))
        }
    }

    override fun getStockDetail(symbol: String): Flow<Resource<Stock>> = flow {
        emit(Resource.Loading)
        try {
            val dto = remoteDataSource.getStockDetail(symbol)
                ?: return@flow emit(Resource.Error("$symbol bulunamadı"))
            emit(Resource.Success(dto.toDomain()))
        } catch (e: Exception) {
            emit(Resource.Error(message = e.localizedMessage ?: "Bilinmeyen hata", throwable = e))
        }
    }

    override fun getBatchStocks(symbols: List<String>): Flow<Resource<List<Stock>>> = flow {
        emit(Resource.Loading)
        try {
            val dtos = remoteDataSource.getBatchStocks(symbols)
            emit(Resource.Success(dtos.toDomain()))
        } catch (e: Exception) {
            emit(Resource.Error(message = e.localizedMessage ?: "Bilinmeyen hata", throwable = e))
        }
    }
}

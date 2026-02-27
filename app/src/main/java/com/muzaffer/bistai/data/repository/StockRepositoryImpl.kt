package com.muzaffer.bistai.data.repository

import com.muzaffer.bistai.data.local.fake.FakeStockDataSource
import com.muzaffer.bistai.data.mapper.toDomain
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
 * Şu an: FakeDataSource'u kullanır (gerçek API anahtarı olmadan test için).
 * Hazır olduğunda: [apiService]'i aktif hale getirip [fakeDataSource] kullanımını kaldır.
 */
class StockRepositoryImpl @Inject constructor(
    private val apiService: StockApiService,
    private val fakeDataSource: FakeStockDataSource
) : StockRepository {

    override fun getStocks(): Flow<Resource<List<Stock>>> = flow {
        emit(Resource.Loading)
        try {
            // TODO: Gerçek API hazır olunca → apiService.getStocks() kullan
            val dtos = fakeDataSource.getStocks()
            emit(Resource.Success(dtos.toDomain()))
        } catch (e: Exception) {
            emit(Resource.Error(message = e.localizedMessage ?: "Bilinmeyen hata", throwable = e))
        }
    }

    override fun getStockDetail(symbol: String): Flow<Resource<Stock>> = flow {
        emit(Resource.Loading)
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
            val dtos = fakeDataSource.getBatchStocks(symbols)
            emit(Resource.Success(dtos.toDomain()))
        } catch (e: Exception) {
            emit(Resource.Error(message = e.localizedMessage ?: "Bilinmeyen hata", throwable = e))
        }
    }
}

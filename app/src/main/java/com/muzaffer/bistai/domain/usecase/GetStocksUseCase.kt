package com.muzaffer.bistai.domain.usecase

import com.muzaffer.bistai.domain.model.Stock
import com.muzaffer.bistai.domain.repository.StockRepository
import com.muzaffer.bistai.domain.util.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Hisse listesini domain sözleşmesi üzerinden getiren tek sorumluluklu use case.
 *
 * ViewModel sadece bu sınıfı çağırır; veri kaynağı (fake/gerçek API)
 * bu katmanın arkasında tamamen gizlidir.
 */
class GetStocksUseCase @Inject constructor(
    private val repository: StockRepository
) {
    operator fun invoke(): Flow<Resource<List<Stock>>> = repository.getStocks()
}

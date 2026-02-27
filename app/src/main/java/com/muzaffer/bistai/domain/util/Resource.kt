package com.muzaffer.bistai.domain.util

/**
 * Ağ/veritabanı işlemlerinin sonucunu taşıyan generic sarmalayıcı.
 *
 * Kullanım:
 *  when (result) {
 *      is Resource.Success -> showData(result.data)
 *      is Resource.Error   -> showError(result.message)
 *      is Resource.Loading -> showLoader()
 *  }
 */
sealed class Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : Resource<Nothing>()
    data object Loading : Resource<Nothing>()

    val isSuccess get() = this is Success
    val isError   get() = this is Error
    val isLoading  get() = this is Loading
}

package com.muzaffer.bistai.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.muzaffer.bistai.core.Constants
import com.muzaffer.bistai.data.local.dao.PortfolioDao
import com.muzaffer.bistai.data.local.entity.PortfolioEntity
import com.muzaffer.bistai.domain.model.PortfolioItem
import com.muzaffer.bistai.domain.repository.PortfolioRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PortfolioRepositoryImpl @Inject constructor(
    private val portfolioDao: PortfolioDao,
    private val firestore: FirebaseFirestore
) : PortfolioRepository {

    override fun getPortfolio(uid: String?): Flow<List<PortfolioItem>> {
        if (uid.isNullOrBlank()) {
            return portfolioDao.getAllPortfolioItems().map { entities ->
                entities.map { it.toDomain() }
            }
        }

        return channelFlow {
            val collectionRef = firestore.collection(Constants.USERS_COLLECTION)
                .document(uid)
                .collection(Constants.PORTFOLIO_COLLECTION)

            val subscription = collectionRef.addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val cloudItems = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(PortfolioItem::class.java)?.copy(symbol = doc.id)
                    }
                    if (cloudItems.isNotEmpty()) {
                        launch {
                            portfolioDao.upsertAll(cloudItems.map { PortfolioEntity.fromDomain(it) })
                        }
                    }
                }
            }

            val localJob = launch {
                portfolioDao.getAllPortfolioItems().collect { entities ->
                    send(entities.map { it.toDomain() })
                }
            }

            awaitClose {
                subscription.remove()
                localJob.cancel()
            }
        }
    }

    override suspend fun addOrUpdatePortfolioItem(uid: String?, item: PortfolioItem): Result<Unit> {
        return try {
            portfolioDao.upsertPortfolioItem(PortfolioEntity.fromDomain(item))
            if (!uid.isNullOrBlank()) {
                firestore.collection(Constants.USERS_COLLECTION)
                    .document(uid)
                    .collection(Constants.PORTFOLIO_COLLECTION)
                    .document(item.symbol)
                    .set(item)
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removePortfolioItem(uid: String?, symbol: String): Result<Unit> {
        return try {
            portfolioDao.deletePortfolioItem(symbol)
            if (!uid.isNullOrBlank()) {
                firestore.collection(Constants.USERS_COLLECTION)
                    .document(uid)
                    .collection(Constants.PORTFOLIO_COLLECTION)
                    .document(symbol)
                    .delete()
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncLocalToCloud(uid: String): Result<Unit> {
        return try {
            val localItems = portfolioDao.getAllPortfolioItemsOnce()
            val collectionRef = firestore.collection(Constants.USERS_COLLECTION)
                .document(uid)
                .collection(Constants.PORTFOLIO_COLLECTION)

            localItems.forEach { entity ->
                collectionRef.document(entity.symbol)
                    .set(entity.toDomain())
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

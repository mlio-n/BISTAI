package com.muzaffer.bistai.data.repository

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
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

    private fun portfolioCollection(uid: String): CollectionReference =
        firestore.collection(Constants.USERS_COLLECTION)
            .document(uid)
            .collection(Constants.PORTFOLIO_COLLECTION)

    override fun getPortfolio(uid: String?): Flow<List<PortfolioItem>> {
        if (uid.isNullOrBlank()) {
            return portfolioDao.getAllPortfolioItems().map { entities ->
                entities.map { it.toDomain() }
            }
        }

        return channelFlow {
            val localJob = launch {
                portfolioDao.getAllPortfolioItems().collect { entities ->
                    send(entities.map { it.toDomain() })
                }
            }

            syncLocalToCloud(uid)

            val collectionRef = portfolioCollection(uid)
            val subscription = collectionRef.addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val cloudItems = snapshot.documents.mapNotNull { it.toPortfolioItem() }
                launch {
                    if (cloudItems.isNotEmpty()) {
                        portfolioDao.upsertAll(cloudItems.map { PortfolioEntity.fromDomain(it) })
                        if (!snapshot.metadata.isFromCache && !snapshot.metadata.hasPendingWrites()) {
                            portfolioDao.deleteItemsNotIn(cloudItems.map { it.symbol })
                        }
                    } else if (!snapshot.metadata.isFromCache && !snapshot.metadata.hasPendingWrites()) {
                        portfolioDao.clearAll()
                    }
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
            val normalizedItem = item.copy(symbol = item.symbol.trim().uppercase())
            portfolioDao.upsertPortfolioItem(PortfolioEntity.fromDomain(normalizedItem))
            if (!uid.isNullOrBlank()) {
                portfolioCollection(uid)
                    .document(normalizedItem.symbol)
                    .set(normalizedItem.toFirestoreMap())
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removePortfolioItem(uid: String?, symbol: String): Result<Unit> {
        return try {
            val normalizedSymbol = symbol.trim().uppercase()
            portfolioDao.deletePortfolioItem(normalizedSymbol)
            if (!uid.isNullOrBlank()) {
                portfolioCollection(uid)
                    .document(normalizedSymbol)
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
            val collectionRef = portfolioCollection(uid)
            val localEntities = portfolioDao.getAllPortfolioItemsOnce()
            val remoteSnapshot = collectionRef.get().await()

            val remoteMap = remoteSnapshot.documents
                .mapNotNull { it.toPortfolioItem() }
                .associateBy { it.symbol }

            val batch = firestore.batch()
            var hasBatchWrites = false
            val itemsToUpsertLocally = mutableListOf<PortfolioEntity>()

            localEntities.forEach { localEntity ->
                val normalizedSymbol = localEntity.symbol.trim().uppercase()
                val localItem = localEntity.toDomain().copy(symbol = normalizedSymbol)
                val remoteItem = remoteMap[normalizedSymbol]

                if (remoteItem == null || localItem.addedAt >= remoteItem.addedAt) {
                    batch.set(collectionRef.document(normalizedSymbol), localItem.toFirestoreMap())
                    hasBatchWrites = true
                } else {
                    itemsToUpsertLocally.add(PortfolioEntity.fromDomain(remoteItem))
                }
            }

            val localSymbols = localEntities.map { it.symbol.trim().uppercase() }.toSet()
            remoteMap.values
                .filter { it.symbol !in localSymbols }
                .forEach { remoteOnlyItem ->
                    itemsToUpsertLocally.add(PortfolioEntity.fromDomain(remoteOnlyItem))
                }

            if (hasBatchWrites) {
                batch.commit().await()
            }
            if (itemsToUpsertLocally.isNotEmpty()) {
                portfolioDao.upsertAll(itemsToUpsertLocally)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun DocumentSnapshot.toPortfolioItem(): PortfolioItem? {
        val item = toObject(PortfolioItem::class.java) ?: return null
        val normalizedSymbol = item.symbol.ifBlank { id }.trim().uppercase()
        if (normalizedSymbol.isBlank()) return null
        return item.copy(symbol = normalizedSymbol)
    }

    private fun PortfolioItem.toFirestoreMap(): Map<String, Any> = mapOf(
        "symbol" to symbol,
        "lotSize" to lotSize,
        "averageCost" to averageCost,
        "addedAt" to addedAt
    )
}

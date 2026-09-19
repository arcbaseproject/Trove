package com.trove.app.data.repository

import com.trove.app.data.local.db.dao.CollectionDao
import com.trove.app.data.local.db.entity.CollectionEntity
import com.trove.app.domain.model.CollectionUi
import com.trove.app.util.IoDispatcher
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class CollectionRepositoryImpl @Inject constructor(
    private val collectionDao: CollectionDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CollectionRepository {

    override fun observeCollections(): Flow<List<CollectionUi>> =
        collectionDao.observeAllWithCounts().map { rows ->
            rows.map { CollectionUi(it.id, it.name, it.count, it.coverUri) }
        }

    override suspend fun createCollection(name: String): Long = withContext(ioDispatcher) {
        collectionDao.insert(CollectionEntity(name = name.trim(), createdAtSec = Instant.now().epochSecond))
    }

    override suspend fun deleteCollection(id: Long) = withContext(ioDispatcher) {
        collectionDao.delete(id)
    }
}

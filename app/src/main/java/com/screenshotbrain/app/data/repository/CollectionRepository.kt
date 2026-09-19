package com.trove.app.data.repository

import com.trove.app.domain.model.CollectionUi
import kotlinx.coroutines.flow.Flow

interface CollectionRepository {
    fun observeCollections(): Flow<List<CollectionUi>>
    suspend fun createCollection(name: String): Long
    suspend fun deleteCollection(id: Long)
}

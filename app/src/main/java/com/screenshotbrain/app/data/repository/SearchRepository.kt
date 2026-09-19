package com.trove.app.data.repository

import com.trove.app.domain.model.ScreenshotUi

/**
 * Search entry point used by the UI. [FtsSearchRepository] backs it with SQLite FTS4 today;
 * a later semantic/vector implementation (embeddings + cosine similarity, or a hybrid of both)
 * can implement this same interface and be swapped in via the Hilt binding in RepositoryModule
 * without any UI or ViewModel change.
 */
interface SearchRepository {
    suspend fun search(query: String, limit: Int = 200): List<ScreenshotUi>
}

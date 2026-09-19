package com.trove.app.data.repository

import android.app.PendingIntent
import androidx.paging.PagingData
import com.trove.app.data.local.db.dao.CategoryWithCount
import com.trove.app.domain.model.ScreenshotDetail
import com.trove.app.domain.model.ScreenshotUi
import kotlinx.coroutines.flow.Flow

data class DuplicateGroup(val screenshotIds: List<Long>)

/** Result of requesting deletion of user media not owned by this app (API 30+ system flow). */
sealed interface DeleteRequestOutcome {
    data class NeedsConfirmation(val pendingIntent: PendingIntent, val ids: List<Long>) : DeleteRequestOutcome
    data class Deleted(val ids: List<Long>) : DeleteRequestOutcome
}

interface ScreenshotRepository {
    fun pagedScreenshots(): Flow<PagingData<ScreenshotUi>>
    fun pagedScreenshotsByCategory(categoryKey: String): Flow<PagingData<ScreenshotUi>>
    fun pagedScreenshotsByCollection(collectionId: Long): Flow<PagingData<ScreenshotUi>>
    fun recentScreenshots(limit: Int): Flow<List<ScreenshotUi>>

    /** Ordered snapshots for the full-screen swipe viewer; see the DAO comment for why these aren't Paging. */
    suspend fun listAllOnce(limit: Int = 500): List<ScreenshotUi>
    suspend fun listByCategoryOnce(categoryKey: String, limit: Int = 500): List<ScreenshotUi>
    suspend fun listByCollectionOnce(collectionId: Long, limit: Int = 500): List<ScreenshotUi>
    fun observeDetail(screenshotId: Long): Flow<ScreenshotDetail?>
    fun observeTotalCount(): Flow<Int>
    fun observeCategoriesWithCounts(): Flow<List<CategoryWithCount>>

    suspend fun setFavorite(screenshotId: Long, isFavorite: Boolean)
    suspend fun updateNotes(screenshotId: Long, notes: String?)
    suspend fun addTag(screenshotId: Long, tagName: String)
    suspend fun removeTag(screenshotId: Long, tagId: Long)
    suspend fun addToCollection(screenshotId: Long, collectionId: Long)
    suspend fun removeFromCollection(screenshotId: Long, collectionId: Long)

    /** Starts (or resolves) OS-level deletion for [ids]; caller must remove local rows once confirmed. */
    suspend fun requestDelete(ids: List<Long>): DeleteRequestOutcome
    suspend fun removeLocalRows(ids: List<Long>)
    suspend fun resetIndexing()
    suspend fun getContentUris(ids: List<Long>): List<String>
    suspend fun getScreenshotsByIds(ids: List<Long>): List<ScreenshotUi>

    suspend fun findExactDuplicates(): List<DuplicateGroup>
}

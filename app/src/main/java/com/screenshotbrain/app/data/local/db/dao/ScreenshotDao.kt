package com.trove.app.data.local.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.trove.app.data.local.db.entity.ScreenshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScreenshotDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(screenshots: List<ScreenshotEntity>): List<Long>

    @Update
    suspend fun update(screenshot: ScreenshotEntity)

    @Query("SELECT * FROM screenshots WHERE mediaStoreId IN (:mediaStoreIds)")
    suspend fun getByMediaStoreIds(mediaStoreIds: List<Long>): List<ScreenshotEntity>

    @Query("SELECT mediaStoreId FROM screenshots")
    suspend fun getAllMediaStoreIds(): List<Long>

    // search_index is FTS4: no foreign key, so no cascade. Its rows are deleted by hand here.
    @Transaction
    suspend fun deleteByMediaStoreIds(mediaStoreIds: List<Long>) {
        deleteSearchRowsByMediaStoreIds(mediaStoreIds)
        deleteRowsByMediaStoreIds(mediaStoreIds)
    }

    @Transaction
    suspend fun deleteByIds(ids: List<Long>) {
        deleteSearchRowsByIds(ids)
        deleteRowsByIds(ids)
    }

    @Query("DELETE FROM search_index WHERE screenshotId IN (SELECT id FROM screenshots WHERE mediaStoreId IN (:mediaStoreIds))")
    suspend fun deleteSearchRowsByMediaStoreIds(mediaStoreIds: List<Long>)

    @Query("DELETE FROM screenshots WHERE mediaStoreId IN (:mediaStoreIds)")
    suspend fun deleteRowsByMediaStoreIds(mediaStoreIds: List<Long>)

    @Query("DELETE FROM search_index WHERE screenshotId IN (:ids)")
    suspend fun deleteSearchRowsByIds(ids: List<Long>)

    @Query("DELETE FROM screenshots WHERE id IN (:ids)")
    suspend fun deleteRowsByIds(ids: List<Long>)

    @Query("SELECT * FROM screenshots WHERE id = :id")
    fun observeById(id: Long): Flow<ScreenshotEntity?>

    @Query("SELECT * FROM screenshots WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<ScreenshotEntity>

    @Query("SELECT * FROM screenshots ORDER BY dateAddedSec DESC")
    fun pagingSourceAll(): PagingSource<Int, ScreenshotEntity>

    @Query("SELECT * FROM screenshots ORDER BY dateAddedSec DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ScreenshotEntity>>

    @Query(
        """
        SELECT s.* FROM screenshots s
        INNER JOIN screenshot_category_cross_ref c ON c.screenshotId = s.id
        WHERE c.categoryKey = :categoryKey
        ORDER BY s.dateAddedSec DESC
        """,
    )
    fun pagingSourceByCategory(categoryKey: String): PagingSource<Int, ScreenshotEntity>

    @Query(
        """
        SELECT s.* FROM screenshots s
        INNER JOIN collection_screenshot_cross_ref cr ON cr.screenshotId = s.id
        WHERE cr.collectionId = :collectionId
        ORDER BY cr.addedAtSec DESC
        """,
    )
    fun pagingSourceByCollection(collectionId: Long): PagingSource<Int, ScreenshotEntity>

    // ponytail: the full-screen swipe viewer needs an ordered in-memory list (Pager doesn't
    // speak PagingData), so these mirror the paging queries above but return a capped plain
    // list instead. 500 items of metadata is negligible memory; unbounded swipe through 10k+
    // screenshots would need a paged HorizontalPager, not needed for the common "tap, swipe a
    // few, back out" flow.
    @Query("SELECT * FROM screenshots ORDER BY dateAddedSec DESC LIMIT :limit")
    suspend fun getAllOnce(limit: Int): List<ScreenshotEntity>

    @Query(
        """
        SELECT s.* FROM screenshots s
        INNER JOIN screenshot_category_cross_ref c ON c.screenshotId = s.id
        WHERE c.categoryKey = :categoryKey
        ORDER BY s.dateAddedSec DESC LIMIT :limit
        """,
    )
    suspend fun getByCategoryOnce(categoryKey: String, limit: Int): List<ScreenshotEntity>

    @Query(
        """
        SELECT s.* FROM screenshots s
        INNER JOIN collection_screenshot_cross_ref cr ON cr.screenshotId = s.id
        WHERE cr.collectionId = :collectionId
        ORDER BY cr.addedAtSec DESC LIMIT :limit
        """,
    )
    suspend fun getByCollectionOnce(collectionId: Long, limit: Int): List<ScreenshotEntity>

    @Query("SELECT * FROM screenshots WHERE ocrProcessed = 0 AND id NOT IN (:excludeIds) LIMIT :limit")
    suspend fun getUnprocessedForOcr(limit: Int, excludeIds: Collection<Long>): List<ScreenshotEntity>

    @Query("SELECT * FROM screenshots WHERE classified = 0 LIMIT :limit")
    suspend fun getUnclassified(limit: Int): List<ScreenshotEntity>

    @Query("SELECT COUNT(*) FROM screenshots")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM screenshots")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM screenshots WHERE ocrProcessed = 1")
    suspend fun getProcessedCount(): Int

    /** Marks every screenshot for OCR and classification again. Tags, notes and favorites stay. */
    @Query("UPDATE screenshots SET ocrProcessed = 0, classified = 0")
    suspend fun resetIndexing()

    @Query("UPDATE screenshots SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE screenshots SET notes = :notes WHERE id = :id")
    suspend fun setNotes(id: Long, notes: String?)

    @Query("UPDATE screenshots SET ocrProcessed = 1 WHERE id = :id")
    suspend fun markOcrProcessed(id: Long)

    @Query("UPDATE screenshots SET classified = 1 WHERE id = :id")
    suspend fun markClassified(id: Long)

    @Delete
    suspend fun delete(screenshot: ScreenshotEntity)

    @Query(
        """
        SELECT sizeBytes || ':' || width || ':' || height AS signature, GROUP_CONCAT(id) AS ids, COUNT(*) AS count
        FROM screenshots
        WHERE sizeBytes > 0 AND width > 0 AND height > 0
        GROUP BY signature
        HAVING count > 1
        """,
    )
    suspend fun findExactDuplicateGroups(): List<DuplicateGroupRow>
}

data class DuplicateGroupRow(
    val signature: String,
    val ids: String,
    val count: Int,
)

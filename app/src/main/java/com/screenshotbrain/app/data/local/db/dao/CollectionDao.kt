package com.trove.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.trove.app.data.local.db.entity.CollectionEntity
import com.trove.app.data.local.db.entity.CollectionScreenshotCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {

    @Insert
    suspend fun insert(collection: CollectionEntity): Long

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun delete(id: Long)

    @Query(
        """
        SELECT co.id AS id, co.name AS name, co.createdAtSec AS createdAtSec,
               COUNT(cr.screenshotId) AS count,
               (
                   SELECT s.contentUri FROM collection_screenshot_cross_ref cr2
                   INNER JOIN screenshots s ON s.id = cr2.screenshotId
                   WHERE cr2.collectionId = co.id
                   ORDER BY cr2.addedAtSec DESC LIMIT 1
               ) AS coverUri
        FROM collections co
        LEFT JOIN collection_screenshot_cross_ref cr ON cr.collectionId = co.id
        GROUP BY co.id
        ORDER BY co.createdAtSec DESC
        """,
    )
    fun observeAllWithCounts(): Flow<List<CollectionWithCount>>

    @Query("SELECT * FROM collections WHERE id = :id")
    fun observeById(id: Long): Flow<CollectionEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addScreenshot(ref: CollectionScreenshotCrossRef)

    @Query("DELETE FROM collection_screenshot_cross_ref WHERE collectionId = :collectionId AND screenshotId = :screenshotId")
    suspend fun removeScreenshot(collectionId: Long, screenshotId: Long)

    @Query(
        """
        SELECT co.* FROM collections co
        INNER JOIN collection_screenshot_cross_ref cr ON cr.collectionId = co.id
        WHERE cr.screenshotId = :screenshotId
        """,
    )
    fun observeForScreenshot(screenshotId: Long): Flow<List<CollectionEntity>>
}

data class CollectionWithCount(
    val id: Long,
    val name: String,
    val createdAtSec: Long,
    val count: Int,
    val coverUri: String?,
)

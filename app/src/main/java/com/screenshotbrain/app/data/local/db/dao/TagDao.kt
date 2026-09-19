package com.trove.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.trove.app.data.local.db.entity.ScreenshotTagCrossRef
import com.trove.app.data.local.db.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: TagEntity): Long

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): TagEntity?

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun observeAll(): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addToScreenshot(ref: ScreenshotTagCrossRef)

    @Query("DELETE FROM screenshot_tag_cross_ref WHERE screenshotId = :screenshotId AND tagId = :tagId")
    suspend fun removeFromScreenshot(screenshotId: Long, tagId: Long)

    @Query(
        """
        SELECT t.* FROM tags t
        INNER JOIN screenshot_tag_cross_ref r ON r.tagId = t.id
        WHERE r.screenshotId = :screenshotId
        ORDER BY t.name ASC
        """,
    )
    fun observeForScreenshot(screenshotId: Long): Flow<List<TagEntity>>

    @Query(
        """
        SELECT t.* FROM tags t
        INNER JOIN screenshot_tag_cross_ref r ON r.tagId = t.id
        WHERE r.screenshotId = :screenshotId
        ORDER BY t.name ASC
        """,
    )
    suspend fun getForScreenshotOnce(screenshotId: Long): List<TagEntity>
}

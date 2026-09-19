package com.trove.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.trove.app.data.local.db.entity.CategoryEntity
import com.trove.app.data.local.db.entity.ScreenshotCategoryCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun assignCategories(refs: List<ScreenshotCategoryCrossRef>)

    @Query("DELETE FROM screenshot_category_cross_ref WHERE screenshotId = :screenshotId")
    suspend fun clearCategoriesForScreenshot(screenshotId: Long)

    @Query(
        """
        SELECT c.key AS categoryKey, c.displayName AS displayName, c.sortOrder AS sortOrder, COUNT(r.screenshotId) AS count
        FROM categories c
        LEFT JOIN screenshot_category_cross_ref r ON r.categoryKey = c.key
        GROUP BY c.key
        ORDER BY c.sortOrder ASC
        """,
    )
    fun observeAllWithCounts(): Flow<List<CategoryWithCount>>

    @Query(
        """
        SELECT c.key AS categoryKey, c.displayName AS displayName
        FROM categories c
        INNER JOIN screenshot_category_cross_ref r ON r.categoryKey = c.key
        WHERE r.screenshotId = :screenshotId
        """,
    )
    fun observeForScreenshot(screenshotId: Long): Flow<List<CategoryRef>>

    @Query(
        """
        SELECT c.key AS categoryKey, c.displayName AS displayName
        FROM categories c
        INNER JOIN screenshot_category_cross_ref r ON r.categoryKey = c.key
        WHERE r.screenshotId = :screenshotId
        """,
    )
    suspend fun getForScreenshotOnce(screenshotId: Long): List<CategoryRef>
}

data class CategoryWithCount(
    val categoryKey: String,
    val displayName: String,
    val sortOrder: Int,
    val count: Int,
)

data class CategoryRef(
    val categoryKey: String,
    val displayName: String,
)

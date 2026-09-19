package com.trove.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Fixed set of categories seeded on first launch. See [com.trove.app.domain.model.CategoryKey]. */
@Entity(tableName = "categories", indices = [Index(value = ["key"], unique = true)])
data class CategoryEntity(
    @PrimaryKey
    val key: String,
    val displayName: String,
    val sortOrder: Int,
)

/** Many-to-many: a screenshot can belong to several categories. */
@Entity(
    tableName = "screenshot_category_cross_ref",
    primaryKeys = ["screenshotId", "categoryKey"],
    foreignKeys = [
        ForeignKey(
            entity = ScreenshotEntity::class,
            parentColumns = ["id"],
            childColumns = ["screenshotId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["key"],
            childColumns = ["categoryKey"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["categoryKey"]), Index(value = ["screenshotId"])],
)
data class ScreenshotCategoryCrossRef(
    val screenshotId: Long,
    val categoryKey: String,
)

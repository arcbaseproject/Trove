package com.trove.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAtSec: Long,
)

/**
 * A screenshot can live in multiple collections; the underlying media file is never
 * duplicated, only this reference row.
 */
@Entity(
    tableName = "collection_screenshot_cross_ref",
    primaryKeys = ["collectionId", "screenshotId"],
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ScreenshotEntity::class,
            parentColumns = ["id"],
            childColumns = ["screenshotId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["screenshotId"]), Index(value = ["collectionId"])],
)
data class CollectionScreenshotCrossRef(
    val collectionId: Long,
    val screenshotId: Long,
    val addedAtSec: Long,
)

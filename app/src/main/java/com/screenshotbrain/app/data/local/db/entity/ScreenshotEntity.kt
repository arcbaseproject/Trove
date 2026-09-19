package com.trove.app.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per screenshot found on the device. Mirrors MediaStore metadata; never stores
 * pixel data, only a reference (contentUri) that MediaStore / Coil resolve on demand.
 */
@Entity(
    tableName = "screenshots",
    indices = [
        Index(value = ["mediaStoreId"], unique = true),
        Index(value = ["dateAddedSec"]),
        Index(value = ["ocrProcessed"]),
        Index(value = ["classified"]),
        Index(value = ["isFavorite"]),
    ],
)
data class ScreenshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaStoreId: Long,
    val contentUri: String,
    val displayName: String,
    val dateAddedSec: Long,
    val dateModifiedSec: Long,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val mimeType: String,
    val sourceAppPackage: String?,
    val sourceAppLabel: String?,
    val bucketPath: String?,
    @ColumnInfo(defaultValue = "0")
    val isFavorite: Boolean = false,
    val notes: String? = null,
    @ColumnInfo(defaultValue = "0")
    val ocrProcessed: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val classified: Boolean = false,
)

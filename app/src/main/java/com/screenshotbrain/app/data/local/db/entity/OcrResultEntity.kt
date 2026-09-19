package com.trove.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Extracted text for a screenshot. Kept separate from [ScreenshotEntity] so OCR can be
 * re-run (e.g. a better engine ships later) without touching the core screenshot row.
 */
@Entity(
    tableName = "ocr_results",
    foreignKeys = [
        ForeignKey(
            entity = ScreenshotEntity::class,
            parentColumns = ["id"],
            childColumns = ["screenshotId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["screenshotId"], unique = true)],
)
data class OcrResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val screenshotId: Long,
    val text: String,
    val textLength: Int,
    val processedAtSec: Long,
    val engineVersion: Int,
)

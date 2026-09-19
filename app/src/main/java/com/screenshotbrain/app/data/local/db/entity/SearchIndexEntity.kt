package com.trove.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.Fts4

/**
 * Denormalized full-text index, one row per screenshot, rebuilt whenever OCR text,
 * categories, tags, or notes change. Kept separate from [ScreenshotEntity] so the search
 * backend can be swapped (e.g. for a vector/semantic index) behind [com.trove.app.data.repository.SearchRepository]
 * without touching the source tables.
 */
@Fts4
@Entity(tableName = "search_index")
data class SearchIndexEntity(
    val screenshotId: Long,
    val ocrText: String,
    val displayName: String,
    val categoryNames: String,
    val tagNames: String,
    val notes: String,
    val sourceAppLabel: String,
)

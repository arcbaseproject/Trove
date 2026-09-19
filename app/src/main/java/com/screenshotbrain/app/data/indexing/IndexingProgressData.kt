package com.trove.app.data.indexing

import androidx.work.Data
import androidx.work.workDataOf

/** Wire format for [androidx.work.WorkInfo] progress, read back by [IndexingManager]. */
object IndexingProgressData {
    private const val KEY_PROCESSED = "processed"
    private const val KEY_TOTAL = "total"

    fun toWorkData(processed: Int, total: Int): Data = workDataOf(
        KEY_PROCESSED to processed,
        KEY_TOTAL to total,
    )

    fun processed(data: Data): Int = data.getInt(KEY_PROCESSED, 0)
    fun total(data: Data): Int = data.getInt(KEY_TOTAL, 0)
}

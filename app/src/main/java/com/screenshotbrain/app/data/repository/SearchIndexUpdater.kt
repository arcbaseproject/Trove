package com.trove.app.data.repository

import com.trove.app.data.local.db.dao.CategoryDao
import com.trove.app.data.local.db.dao.OcrDao
import com.trove.app.data.local.db.dao.ScreenshotDao
import com.trove.app.data.local.db.dao.SearchDao
import com.trove.app.data.local.db.dao.TagDao
import com.trove.app.data.local.db.entity.SearchIndexEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Recomputes the single [SearchIndexEntity] row for a screenshot from its current OCR text,
 * categories, tags and notes. Called after any of those change, from either the indexing
 * pipeline or user edits, so there is one place that knows how the FTS row is assembled.
 */
@Singleton
class SearchIndexUpdater @Inject constructor(
    private val screenshotDao: ScreenshotDao,
    private val ocrDao: OcrDao,
    private val categoryDao: CategoryDao,
    private val tagDao: TagDao,
    private val searchDao: SearchDao,
) {
    suspend fun rebuild(screenshotId: Long) {
        val screenshot = screenshotDao.getByIds(listOf(screenshotId)).firstOrNull() ?: return
        val ocr = ocrDao.getForScreenshot(screenshotId)
        val categories = categoryDao.getForScreenshotOnce(screenshotId)
        val tags = tagDao.getForScreenshotOnce(screenshotId)

        searchDao.upsert(
            SearchIndexEntity(
                screenshotId = screenshotId,
                ocrText = ocr?.text.orEmpty(),
                displayName = screenshot.displayName,
                categoryNames = categories.joinToString(" ") { it.displayName },
                tagNames = tags.joinToString(" ") { it.name },
                notes = screenshot.notes.orEmpty(),
                sourceAppLabel = screenshot.sourceAppLabel.orEmpty(),
            ),
        )
    }
}

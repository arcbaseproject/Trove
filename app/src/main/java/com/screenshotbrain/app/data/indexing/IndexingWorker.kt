package com.trove.app.data.indexing

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.trove.app.data.classification.ClassificationInput
import com.trove.app.data.classification.Classifier
import com.trove.app.data.local.db.dao.CategoryDao
import com.trove.app.data.local.db.dao.OcrDao
import com.trove.app.data.local.db.dao.ScreenshotDao
import com.trove.app.data.local.db.entity.OcrResultEntity
import com.trove.app.data.local.db.entity.ScreenshotCategoryCrossRef
import com.trove.app.data.local.db.entity.ScreenshotEntity
import com.trove.app.data.mediastore.MediaStoreScreenshotSource
import com.trove.app.data.mediastore.toEntity
import com.trove.app.data.ocr.OCR_ENGINE_VERSION
import com.trove.app.data.ocr.OcrEngine
import com.trove.app.data.repository.SearchIndexUpdater
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Instant

private const val BATCH_SIZE = 12

/**
 * Resumable background pipeline: (1) sync MediaStore -> local rows, (2) OCR whatever isn't
 * processed yet, (3) classify whatever isn't categorized yet. Every step re-queries "what's
 * left to do" from Room instead of tracking progress in memory, so a killed/retried worker
 * just picks up where it left off.
 */
@HiltWorker
class IndexingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val mediaStoreSource: MediaStoreScreenshotSource,
    private val screenshotDao: ScreenshotDao,
    private val ocrDao: OcrDao,
    private val categoryDao: CategoryDao,
    private val ocrEngine: OcrEngine,
    private val classifier: Classifier,
    private val searchIndexUpdater: SearchIndexUpdater,
    private val indexingManager: IndexingManager,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!mediaStoreSource.hasReadPermission()) return Result.success()
        // All indexing runs as the one-time unique job, so runs never overlap and the progress banner sees them.
        // shortcut: the forwarded run ignores the periodic job's charging constraint; pass constraints through if that matters.
        if (indexingManager.isPeriodicRun(id)) {
            indexingManager.runNow()
            return Result.success()
        }

        syncMediaStore()

        val total = screenshotDao.getTotalCount()
        var processed = screenshotDao.getProcessedCount()
        setProgress(IndexingProgressData.toWorkData(processed, total))

        // OCR failures stay unprocessed for the next run; skipping them here keeps this run from looping.
        // shortcut: a permanently broken file costs one failed OCR per run; add an attempt counter column if that shows up.
        val failedIds = mutableSetOf<Long>()
        while (true) {
            val batch = screenshotDao.getUnprocessedForOcr(BATCH_SIZE, failedIds)
            // Stays under SQLite's 999 bind-variable limit; a run with this many failures is broken anyway.
            if (batch.isEmpty() || failedIds.size >= 900) break
            for (screenshot in batch) {
                if (!runOcrAndClassify(screenshot)) failedIds += screenshot.id
                processed++
                setProgress(IndexingProgressData.toWorkData(processed, total))
            }
        }
        return Result.success()
    }

    private suspend fun syncMediaStore() {
        val discovered = mediaStoreSource.queryAllScreenshots()
        val discoveredIds = discovered.map { it.mediaStoreId }.toSet()

        val known = screenshotDao.getAllMediaStoreIds().toSet()
        val removed = known - discoveredIds
        // An empty scan is far more likely a transient MediaStore/storage failure than every screenshot
        // being gone; deleting would cascade away notes, tags and collections.
        // shortcut: a user who truly deletes all screenshots keeps stale rows until one new screenshot appears.
        if (removed.isNotEmpty() && discovered.isNotEmpty()) {
            screenshotDao.deleteByMediaStoreIds(removed.toList())
        }

        val newOnes = discovered.filter { it.mediaStoreId !in known }
        if (newOnes.isNotEmpty()) {
            screenshotDao.insertAll(newOnes.map { it.toEntity() })
        }
    }

    /** Returns false when OCR failed. The screenshot is still classified and indexed by name and source app. */
    private suspend fun runOcrAndClassify(screenshot: ScreenshotEntity): Boolean {
        val outcome = ocrEngine.recognize(Uri.parse(screenshot.contentUri))
        if (outcome.success) {
            ocrDaoUpsert(screenshot.id, outcome.text)
            screenshotDao.markOcrProcessed(screenshot.id)
        }

        val categories = classifier.classify(
            ClassificationInput(
                ocrText = outcome.text,
                displayName = screenshot.displayName,
                sourceAppPackage = screenshot.sourceAppPackage,
                sourceAppLabel = screenshot.sourceAppLabel,
            ),
        )
        categoryDaoAssign(screenshot.id, categories.map { it.name })
        screenshotDao.markClassified(screenshot.id)

        searchIndexUpdater.rebuild(screenshot.id)
        return outcome.success
    }

    private suspend fun ocrDaoUpsert(screenshotId: Long, text: String) {
        ocrDao.upsert(
            OcrResultEntity(
                screenshotId = screenshotId,
                text = text,
                textLength = text.length,
                processedAtSec = Instant.now().epochSecond,
                engineVersion = OCR_ENGINE_VERSION,
            ),
        )
    }

    private suspend fun categoryDaoAssign(screenshotId: Long, categoryKeys: List<String>) {
        categoryDao.clearCategoriesForScreenshot(screenshotId)
        categoryDao.assignCategories(categoryKeys.map { ScreenshotCategoryCrossRef(screenshotId, it) })
    }
}

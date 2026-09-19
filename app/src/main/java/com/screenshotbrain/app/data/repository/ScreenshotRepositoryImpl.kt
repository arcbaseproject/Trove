package com.trove.app.data.repository

import android.app.RecoverableSecurityException
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.trove.app.data.local.db.dao.CategoryDao
import com.trove.app.data.local.db.dao.CollectionDao
import com.trove.app.data.local.db.dao.OcrDao
import com.trove.app.data.local.db.dao.ScreenshotDao
import com.trove.app.data.local.db.dao.TagDao
import com.trove.app.data.local.db.entity.ScreenshotTagCrossRef
import com.trove.app.data.local.db.entity.TagEntity
import com.trove.app.data.local.db.entity.CollectionScreenshotCrossRef
import com.trove.app.domain.model.CategoryRefUi
import com.trove.app.domain.model.CollectionUi
import com.trove.app.domain.model.ScreenshotDetail
import com.trove.app.domain.model.ScreenshotUi
import com.trove.app.domain.model.TagUi
import com.trove.app.domain.model.toUi
import com.trove.app.util.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant

private const val PAGE_SIZE = 60

@Singleton
class ScreenshotRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val screenshotDao: ScreenshotDao,
    private val ocrDao: OcrDao,
    private val categoryDao: CategoryDao,
    private val tagDao: TagDao,
    private val collectionDao: CollectionDao,
    private val searchIndexUpdater: SearchIndexUpdater,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ScreenshotRepository {

    private fun pagerFor(sourceFactory: () -> androidx.paging.PagingSource<Int, com.trove.app.data.local.db.entity.ScreenshotEntity>) =
        Pager(
            config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false, prefetchDistance = PAGE_SIZE / 2),
            pagingSourceFactory = sourceFactory,
        ).flow.map { pagingData -> pagingData.map { it.toUi() } }

    override fun pagedScreenshots(): Flow<PagingData<com.trove.app.domain.model.ScreenshotUi>> =
        pagerFor { screenshotDao.pagingSourceAll() }

    override fun pagedScreenshotsByCategory(categoryKey: String) =
        pagerFor { screenshotDao.pagingSourceByCategory(categoryKey) }

    override fun pagedScreenshotsByCollection(collectionId: Long) =
        pagerFor { screenshotDao.pagingSourceByCollection(collectionId) }

    override fun recentScreenshots(limit: Int) =
        screenshotDao.observeRecent(limit).map { list -> list.map { it.toUi() } }

    override suspend fun listAllOnce(limit: Int) = withContext(ioDispatcher) {
        screenshotDao.getAllOnce(limit).map { it.toUi() }
    }

    override suspend fun listByCategoryOnce(categoryKey: String, limit: Int) = withContext(ioDispatcher) {
        screenshotDao.getByCategoryOnce(categoryKey, limit).map { it.toUi() }
    }

    override suspend fun listByCollectionOnce(collectionId: Long, limit: Int) = withContext(ioDispatcher) {
        screenshotDao.getByCollectionOnce(collectionId, limit).map { it.toUi() }
    }

    override fun observeTotalCount(): Flow<Int> = screenshotDao.observeTotalCount()

    override fun observeCategoriesWithCounts() = categoryDao.observeAllWithCounts()

    override fun observeDetail(screenshotId: Long): Flow<ScreenshotDetail?> = combine(
        screenshotDao.observeById(screenshotId),
        ocrDao.observeForScreenshot(screenshotId),
        categoryDao.observeForScreenshot(screenshotId),
        tagDao.observeForScreenshot(screenshotId),
        collectionDao.observeForScreenshot(screenshotId),
    ) { screenshot, ocr, categories, tags, collections ->
        if (screenshot == null) return@combine null

        ScreenshotDetail(
            screenshot = screenshot.toUi(),
            sizeBytes = screenshot.sizeBytes,
            mimeType = screenshot.mimeType,
            sourceAppLabel = screenshot.sourceAppLabel,
            ocrText = ocr?.text,
            categories = categories.map { CategoryRefUi(it.categoryKey, it.displayName) },
            tags = tags.map { TagUi(it.id, it.name) },
            notes = screenshot.notes,
            collections = collections.map { CollectionUi(it.id, it.name, count = 0, coverUri = null) },
        )
    }

    override suspend fun setFavorite(screenshotId: Long, isFavorite: Boolean) = withContext(ioDispatcher) {
        screenshotDao.setFavorite(screenshotId, isFavorite)
    }

    override suspend fun updateNotes(screenshotId: Long, notes: String?) = withContext(ioDispatcher) {
        screenshotDao.setNotes(screenshotId, notes)
        searchIndexUpdater.rebuild(screenshotId)
    }

    override suspend fun addTag(screenshotId: Long, tagName: String) = withContext(ioDispatcher) {
        val trimmed = tagName.trim()
        if (trimmed.isEmpty()) return@withContext
        val existing = tagDao.findByName(trimmed)
        val tagId = existing?.id ?: run {
            tagDao.insert(TagEntity(name = trimmed))
            tagDao.findByName(trimmed)?.id ?: return@withContext
        }
        tagDao.addToScreenshot(ScreenshotTagCrossRef(screenshotId, tagId))
        searchIndexUpdater.rebuild(screenshotId)
    }

    override suspend fun removeTag(screenshotId: Long, tagId: Long) = withContext(ioDispatcher) {
        tagDao.removeFromScreenshot(screenshotId, tagId)
        searchIndexUpdater.rebuild(screenshotId)
    }

    override suspend fun addToCollection(screenshotId: Long, collectionId: Long) = withContext(ioDispatcher) {
        collectionDao.addScreenshot(
            CollectionScreenshotCrossRef(collectionId, screenshotId, Instant.now().epochSecond),
        )
    }

    override suspend fun removeFromCollection(screenshotId: Long, collectionId: Long) = withContext(ioDispatcher) {
        collectionDao.removeScreenshot(collectionId, screenshotId)
    }

    override suspend fun requestDelete(ids: List<Long>): DeleteRequestOutcome = withContext(ioDispatcher) {
        val entities = screenshotDao.getByIds(ids)
        val uris = entities.map { Uri.parse(it.contentUri) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, uris)
            return@withContext DeleteRequestOutcome.NeedsConfirmation(pendingIntent, ids)
        }

        // ponytail: pre-API30 has no batch system confirmation; delete what we can directly and
        // surface the first recoverable-security prompt if the OS blocks one (API 29 only).
        val deleted = mutableListOf<Long>()
        for ((entity, uri) in entities.zip(uris)) {
            try {
                context.contentResolver.delete(uri, null, null)
                deleted += entity.id
            } catch (e: RecoverableSecurityException) {
                return@withContext DeleteRequestOutcome.NeedsConfirmation(e.userAction.actionIntent, listOf(entity.id))
            }
        }
        DeleteRequestOutcome.Deleted(deleted)
    }

    override suspend fun removeLocalRows(ids: List<Long>) = withContext(ioDispatcher) {
        screenshotDao.deleteByIds(ids)
    }

    override suspend fun resetIndexing() = withContext(ioDispatcher) {
        screenshotDao.resetIndexing()
    }

    override suspend fun getContentUris(ids: List<Long>): List<String> = withContext(ioDispatcher) {
        screenshotDao.getByIds(ids).map { it.contentUri }
    }

    override suspend fun getScreenshotsByIds(ids: List<Long>): List<ScreenshotUi> = withContext(ioDispatcher) {
        screenshotDao.getByIds(ids).map { it.toUi() }
    }

    override suspend fun findExactDuplicates(): List<DuplicateGroup> = withContext(ioDispatcher) {
        screenshotDao.findExactDuplicateGroups().map { row ->
            DuplicateGroup(row.ids.split(",").map { it.trim().toLong() })
        }
    }
}

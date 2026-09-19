package com.trove.app.data.repository

import com.trove.app.data.local.db.dao.ScreenshotDao
import com.trove.app.data.local.db.dao.SearchDao
import com.trove.app.domain.model.ScreenshotUi
import com.trove.app.domain.model.toUi
import com.trove.app.util.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Singleton
class FtsSearchRepository @Inject constructor(
    private val searchDao: SearchDao,
    private val screenshotDao: ScreenshotDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SearchRepository {

    override suspend fun search(query: String, limit: Int): List<ScreenshotUi> = withContext(ioDispatcher) {
        val matchQuery = FtsQuerySanitizer.toMatchQuery(query) ?: return@withContext emptyList()
        val ids = searchDao.matchScreenshotIds(matchQuery, limit)
        if (ids.isEmpty()) return@withContext emptyList()
        screenshotDao.getByIds(ids)
            .sortedByDescending { it.dateAddedSec }
            .map { it.toUi() }
    }
}

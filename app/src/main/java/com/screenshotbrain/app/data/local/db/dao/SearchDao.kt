package com.trove.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.trove.app.data.local.db.entity.SearchIndexEntity

@Dao
interface SearchDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: SearchIndexEntity)

    @Query("DELETE FROM search_index WHERE screenshotId = :screenshotId")
    suspend fun delete(screenshotId: Long)

    /**
     * FTS4 match, newest screenshots first, so [limit] cuts the oldest. [matchQuery] is expected to already be sanitized/prefixed by
     * [com.trove.app.data.repository.FtsQuerySanitizer]. Swapping this for a
     * vector-similarity lookup later only requires a new SearchRepository implementation —
     * callers only see screenshot ids, never SQL.
     */
    @Query(
        """
        SELECT search_index.screenshotId FROM search_index
        INNER JOIN screenshots s ON s.id = search_index.screenshotId
        WHERE search_index MATCH :matchQuery
        ORDER BY s.dateAddedSec DESC
        LIMIT :limit
        """,
    )
    suspend fun matchScreenshotIds(matchQuery: String, limit: Int): List<Long>
}

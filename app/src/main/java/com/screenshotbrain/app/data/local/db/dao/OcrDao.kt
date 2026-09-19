package com.trove.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.trove.app.data.local.db.entity.OcrResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OcrDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(result: OcrResultEntity)

    @Query("SELECT * FROM ocr_results WHERE screenshotId = :screenshotId")
    fun observeForScreenshot(screenshotId: Long): Flow<OcrResultEntity?>

    @Query("SELECT * FROM ocr_results WHERE screenshotId = :screenshotId")
    suspend fun getForScreenshot(screenshotId: Long): OcrResultEntity?
}

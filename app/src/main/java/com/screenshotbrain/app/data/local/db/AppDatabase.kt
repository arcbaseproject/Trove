package com.trove.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.trove.app.data.local.db.dao.CategoryDao
import com.trove.app.data.local.db.dao.CollectionDao
import com.trove.app.data.local.db.dao.OcrDao
import com.trove.app.data.local.db.dao.ScreenshotDao
import com.trove.app.data.local.db.dao.SearchDao
import com.trove.app.data.local.db.dao.TagDao
import com.trove.app.data.local.db.entity.CategoryEntity
import com.trove.app.data.local.db.entity.CollectionEntity
import com.trove.app.data.local.db.entity.CollectionScreenshotCrossRef
import com.trove.app.data.local.db.entity.OcrResultEntity
import com.trove.app.data.local.db.entity.ScreenshotCategoryCrossRef
import com.trove.app.data.local.db.entity.ScreenshotEntity
import com.trove.app.data.local.db.entity.ScreenshotTagCrossRef
import com.trove.app.data.local.db.entity.SearchIndexEntity
import com.trove.app.data.local.db.entity.TagEntity
import com.trove.app.domain.model.CategoryKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Database(
    entities = [
        ScreenshotEntity::class,
        OcrResultEntity::class,
        CategoryEntity::class,
        ScreenshotCategoryCrossRef::class,
        TagEntity::class,
        ScreenshotTagCrossRef::class,
        CollectionEntity::class,
        CollectionScreenshotCrossRef::class,
        SearchIndexEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun screenshotDao(): ScreenshotDao
    abstract fun ocrDao(): OcrDao
    abstract fun categoryDao(): CategoryDao
    abstract fun tagDao(): TagDao
    abstract fun collectionDao(): CollectionDao
    abstract fun searchDao(): SearchDao

    companion object {
        const val DATABASE_NAME = "screenshot_brain.db"

        fun seedCallback(scope: CoroutineScope, databaseProvider: () -> AppDatabase): Callback =
            object : Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    scope.launch {
                        val categories = CategoryKey.entries.mapIndexed { index, key ->
                            CategoryEntity(key = key.name, displayName = key.displayName, sortOrder = index)
                        }
                        databaseProvider().categoryDao().insertAll(categories)
                    }
                }
            }
    }
}

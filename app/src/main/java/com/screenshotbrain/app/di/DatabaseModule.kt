package com.trove.app.di

import android.content.Context
import androidx.room.Room
import com.trove.app.data.local.db.AppDatabase
import com.trove.app.data.local.db.dao.CategoryDao
import com.trove.app.data.local.db.dao.CollectionDao
import com.trove.app.data.local.db.dao.OcrDao
import com.trove.app.data.local.db.dao.ScreenshotDao
import com.trove.app.data.local.db.dao.SearchDao
import com.trove.app.data.local.db.dao.TagDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob())

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        applicationScope: CoroutineScope,
    ): AppDatabase {
        lateinit var instance: AppDatabase
        instance = Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .addCallback(AppDatabase.seedCallback(applicationScope) { instance })
            .build()
        return instance
    }

    @Provides
    fun provideScreenshotDao(db: AppDatabase): ScreenshotDao = db.screenshotDao()

    @Provides
    fun provideOcrDao(db: AppDatabase): OcrDao = db.ocrDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideTagDao(db: AppDatabase): TagDao = db.tagDao()

    @Provides
    fun provideCollectionDao(db: AppDatabase): CollectionDao = db.collectionDao()

    @Provides
    fun provideSearchDao(db: AppDatabase): SearchDao = db.searchDao()
}

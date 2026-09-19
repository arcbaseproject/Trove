package com.trove.app.di

import com.trove.app.data.classification.Classifier
import com.trove.app.data.classification.RuleBasedClassifier
import com.trove.app.data.ocr.MlKitOcrEngine
import com.trove.app.data.ocr.OcrEngine
import com.trove.app.data.repository.CollectionRepository
import com.trove.app.data.repository.CollectionRepositoryImpl
import com.trove.app.data.repository.FtsSearchRepository
import com.trove.app.data.repository.ScreenshotRepository
import com.trove.app.data.repository.ScreenshotRepositoryImpl
import com.trove.app.data.repository.SearchRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindScreenshotRepository(impl: ScreenshotRepositoryImpl): ScreenshotRepository

    @Binds
    @Singleton
    abstract fun bindSearchRepository(impl: FtsSearchRepository): SearchRepository

    @Binds
    @Singleton
    abstract fun bindCollectionRepository(impl: CollectionRepositoryImpl): CollectionRepository

    @Binds
    @Singleton
    abstract fun bindOcrEngine(impl: MlKitOcrEngine): OcrEngine

    @Binds
    @Singleton
    abstract fun bindClassifier(impl: RuleBasedClassifier): Classifier
}

package com.trove.app.ui.settings

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import com.trove.app.data.indexing.IndexingManager
import com.trove.app.data.repository.ScreenshotRepository
import com.trove.app.data.settings.SettingsRepository
import com.trove.app.data.settings.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Current preferences for any screen, provided once at the root. */
val LocalUserPreferences = compositionLocalOf { UserPreferences() }

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val screenshotRepository: ScreenshotRepository,
    private val indexingManager: IndexingManager,
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = settingsRepository.preferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserPreferences(),
    )

    fun <T> set(key: Preferences.Key<T>, value: T) = viewModelScope.launch { settingsRepository.set(key, value) }

    fun resetSettings() = viewModelScope.launch { settingsRepository.reset() }

    fun rescanNow() = indexingManager.runNow()

    /** Runs OCR and classification on every screenshot again. */
    fun rereadAll() = viewModelScope.launch {
        screenshotRepository.resetIndexing()
        indexingManager.runNow()
    }

    @OptIn(ExperimentalCoilApi::class)
    fun clearImageCache() = viewModelScope.launch {
        context.imageLoader.memoryCache?.clear()
        withContext(Dispatchers.IO) { context.imageLoader.diskCache?.clear() }
    }
}

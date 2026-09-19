package com.trove.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.trove.app.data.indexing.IndexingManager
import com.trove.app.data.settings.SettingsRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltAndroidApp
class ScreenshotBrainApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var indexingManager: IndexingManager
    @Inject lateinit var settingsRepository: SettingsRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        indexingManager.runNow()
        // Reschedule background scans whenever the user changes the interval or charging rule.
        appScope.launch {
            settingsRepository.preferences
                .map { it.autoScanHours to it.scanOnlyWhileCharging }
                .distinctUntilChanged()
                .collect { (hours, charging) -> indexingManager.schedulePeriodic(hours, charging) }
        }
    }
}

package com.trove.app.data.indexing

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.trove.app.domain.model.IndexingProgress
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val UNIQUE_WORK_NAME = "screenshot_indexing"
private const val PERIODIC_WORK_NAME = "screenshot_indexing_periodic"

/** Owns scheduling of [IndexingWorker]; the rest of the app only sees [progress] and [runNow]. */
@Singleton
class IndexingManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val workManager get() = WorkManager.getInstance(context)

    fun runNow() {
        val request = OneTimeWorkRequestBuilder<IndexingWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        workManager.enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    /** [hours] of 0 cancels background scanning. */
    fun schedulePeriodic(hours: Int, onlyWhileCharging: Boolean) {
        if (hours <= 0) {
            workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<IndexingWorker>(hours.toLong(), TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiresCharging(onlyWhileCharging).build())
            .build()
        workManager.enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    val progress: Flow<IndexingProgress> =
        workManager.getWorkInfosForUniqueWorkFlow(UNIQUE_WORK_NAME).map { infos ->
            val active = infos.firstOrNull { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
            if (active == null) {
                IndexingProgress(processed = 0, total = 0, isRunning = false)
            } else {
                IndexingProgress(
                    processed = IndexingProgressData.processed(active.progress),
                    total = IndexingProgressData.total(active.progress),
                    isRunning = active.state == WorkInfo.State.RUNNING,
                )
            }
        }
}

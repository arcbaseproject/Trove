package com.trove.app.ui.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

/** The single runtime permission this app asks for, resolved per API level. */
val screenshotsPermission: String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

class PermissionState internal constructor(
    granted: Boolean,
    private val activity: Activity?,
    private val launcher: androidx.activity.result.ActivityResultLauncher<String>,
) {
    var isGranted by mutableStateOf(granted)
        private set

    /** True once the user has denied without a rationale prompt being showable — i.e. "don't ask again". */
    var isPermanentlyDenied by mutableStateOf(false)
        private set

    fun request() = launcher.launch(screenshotsPermission)

    internal fun refresh(context: Context) {
        if (ContextCompat.checkSelfPermission(context, screenshotsPermission) == PackageManager.PERMISSION_GRANTED) {
            onResult(true)
        }
    }

    internal fun onResult(granted: Boolean) {
        isGranted = granted
        if (!granted && activity != null) {
            isPermanentlyDenied = !ActivityCompat.shouldShowRequestPermissionRationale(activity, screenshotsPermission)
        }
    }
}

@Composable
fun rememberScreenshotsPermissionState(onGranted: () -> Unit = {}): PermissionState {
    val context = LocalContext.current
    val activity = context as? Activity
    val initiallyGranted = ContextCompat.checkSelfPermission(context, screenshotsPermission) ==
        PackageManager.PERMISSION_GRANTED

    lateinit var state: PermissionState
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        state.onResult(granted)
        if (granted) onGranted()
    }
    state = remember { PermissionState(initiallyGranted, activity, launcher) }
    // Granting from system settings doesn't call the launcher, so check again when the app resumes.
    LifecycleResumeEffect(state) {
        if (!state.isGranted) {
            state.refresh(context)
            if (state.isGranted) onGranted()
        }
        onPauseOrDispose {}
    }
    return state
}

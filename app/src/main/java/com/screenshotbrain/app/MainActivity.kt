package com.trove.app

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.trove.app.data.indexing.IndexingManager
import com.trove.app.data.settings.SettingsKeys
import com.trove.app.data.settings.ThemeMode
import com.trove.app.ui.navigation.ScreenshotBrainNavHost
import com.trove.app.ui.onboarding.OnboardingScreen
import com.trove.app.ui.permission.PermissionScreen
import com.trove.app.ui.permission.rememberScreenshotsPermissionState
import com.trove.app.ui.settings.LocalUserPreferences
import com.trove.app.ui.settings.SettingsViewModel
import com.trove.app.ui.theme.ScreenshotBrainTheme
import com.trove.app.ui.update.UpdatePrompt
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var indexingManager: IndexingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScreenshotBrainRoot(onPermissionGranted = { indexingManager.runNow() })
        }
    }
}

@Composable
private fun ScreenshotBrainRoot(onPermissionGranted: () -> Unit) {
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val preferences by settingsViewModel.preferences.collectAsState()

    ScreenshotBrainTheme(
        darkTheme = when (preferences.themeMode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        },
        dynamicColor = preferences.dynamicColorEnabled,
    ) {
        // Secure screen hides Trove's content in recent apps and blocks screenshots of it.
        val window = (LocalContext.current as Activity).window
        LaunchedEffect(preferences.secureScreen) {
            if (preferences.secureScreen) {
                window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
        val permissionState = rememberScreenshotsPermissionState(onGranted = onPermissionGranted)

        CompositionLocalProvider(LocalUserPreferences provides preferences) {
            Surface(color = MaterialTheme.colorScheme.background) {
                // Onboarding only for people who haven't granted access yet; existing users skip it.
                if (permissionState.isGranted) {
                    ScreenshotBrainNavHost()
                    UpdatePrompt()
                } else if (!preferences.onboardingDone) {
                    OnboardingScreen(
                        onFinish = {
                            settingsViewModel.set(SettingsKeys.ONBOARDING_DONE, true)
                            permissionState.request()
                        },
                    )
                } else {
                    PermissionScreen(
                        permanentlyDenied = permissionState.isPermanentlyDenied,
                        onRequestPermission = permissionState::request,
                    )
                }
            }
        }
    }
}

package com.trove.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColorEnabled: Boolean = false,
    val analyticsEnabled: Boolean = false,
    val gridColumns: Int = 3,
    val timelineView: Boolean = false,
    val showRecentOnHome: Boolean = true,
    val showCategoriesOnHome: Boolean = true,
    /** 0 turns background scanning off. */
    val autoScanHours: Int = 6,
    val scanOnlyWhileCharging: Boolean = false,
    val secureScreen: Boolean = false,
    val onboardingDone: Boolean = false,
)

object SettingsKeys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    val ANALYTICS = booleanPreferencesKey("analytics_enabled")
    val GRID_COLUMNS = intPreferencesKey("grid_columns")
    val TIMELINE_VIEW = booleanPreferencesKey("timeline_view")
    val SHOW_RECENT = booleanPreferencesKey("show_recent_on_home")
    val SHOW_CATEGORIES = booleanPreferencesKey("show_categories_on_home")
    val AUTO_SCAN_HOURS = intPreferencesKey("auto_scan_hours")
    val SCAN_ONLY_CHARGING = booleanPreferencesKey("scan_only_while_charging")
    val SECURE_SCREEN = booleanPreferencesKey("secure_screen")
    val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
}

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val preferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val default = UserPreferences()
        with(SettingsKeys) {
            UserPreferences(
                themeMode = prefs[THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: default.themeMode,
                dynamicColorEnabled = prefs[DYNAMIC_COLOR] ?: default.dynamicColorEnabled,
                analyticsEnabled = prefs[ANALYTICS] ?: default.analyticsEnabled,
                gridColumns = prefs[GRID_COLUMNS] ?: default.gridColumns,
                timelineView = prefs[TIMELINE_VIEW] ?: default.timelineView,
                showRecentOnHome = prefs[SHOW_RECENT] ?: default.showRecentOnHome,
                showCategoriesOnHome = prefs[SHOW_CATEGORIES] ?: default.showCategoriesOnHome,
                autoScanHours = prefs[AUTO_SCAN_HOURS] ?: default.autoScanHours,
                scanOnlyWhileCharging = prefs[SCAN_ONLY_CHARGING] ?: default.scanOnlyWhileCharging,
                secureScreen = prefs[SECURE_SCREEN] ?: default.secureScreen,
                onboardingDone = prefs[ONBOARDING_DONE] ?: default.onboardingDone,
            )
        }
    }

    suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { it[key] = value }
    }

    suspend fun reset() {
        context.dataStore.edit { it.clear() }
    }
}

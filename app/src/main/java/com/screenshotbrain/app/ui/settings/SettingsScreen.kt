package com.trove.app.ui.settings

import com.trove.app.ui.navigation.LocalBottomBarPadding
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.hilt.navigation.compose.hiltViewModel
import com.trove.app.BuildConfig
import com.trove.app.R
import com.trove.app.data.settings.SettingsKeys
import com.trove.app.data.settings.ThemeMode
import com.trove.app.data.settings.UserPreferences
import com.trove.app.ui.components.KitCard
import com.trove.app.ui.components.ScreenHeader
import com.trove.app.ui.components.SegmentedPills
import com.trove.app.ui.theme.Spacing
import com.trove.app.ui.update.UpdateDialogHost
import com.trove.app.ui.update.UpdateViewModel

private val scanIntervals = listOf(0, 1, 6, 24)

/** One page per section; the Settings tab itself only lists these. */
enum class SettingsSection(@StringRes val title: Int, val icon: ImageVector) {
    APPEARANCE(R.string.settings_appearance, Icons.Outlined.Palette),
    LIBRARY(R.string.nav_library, Icons.Outlined.GridView),
    SCANNING(R.string.settings_scanning, Icons.Outlined.DocumentScanner),
    PRIVACY(R.string.privacy_title, Icons.Outlined.Lock),
    MAINTENANCE(R.string.settings_maintenance, Icons.Outlined.Build),
}

@StringRes
private fun themeLabel(mode: ThemeMode) = when (mode) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

@StringRes
private fun scanLabel(hours: Int) = when (hours) {
    0 -> R.string.scan_off
    1 -> R.string.scan_hourly
    6 -> R.string.scan_6h
    else -> R.string.scan_daily
}

/** Current values at a glance, so most visits end on the hub. */
@Composable
private fun summary(section: SettingsSection, preferences: UserPreferences): String = when (section) {
    SettingsSection.APPEARANCE -> stringResource(R.string.settings_theme_summary, stringResource(themeLabel(preferences.themeMode)))
    SettingsSection.LIBRARY -> stringResource(
        R.string.settings_library_summary,
        stringResource(if (preferences.timelineView) R.string.library_timeline else R.string.library_grid),
        preferences.gridColumns,
    )
    SettingsSection.SCANNING -> stringResource(R.string.settings_scan_summary, stringResource(scanLabel(preferences.autoScanHours)))
    SettingsSection.PRIVACY -> stringResource(R.string.settings_privacy_summary)
    SettingsSection.MAINTENANCE -> stringResource(R.string.settings_maintenance_summary)
}

@Composable
fun SettingsScreen(
    onOpenSection: (SettingsSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val preferences by viewModel.preferences.collectAsState()
    val context = LocalContext.current
    val updateViewModel: UpdateViewModel = hiltViewModel()
    val updateState by updateViewModel.uiState.collectAsState()
    // Only a manual check reports "you're up to date"; the launch check stays silent.
    LaunchedEffect(updateState.upToDate) {
        if (updateState.upToDate) context.toast(R.string.settings_up_to_date)
    }

    Scaffold(
        modifier = modifier,
        topBar = { ScreenHeader(title = stringResource(R.string.nav_settings)) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = Spacing.md, end = Spacing.md, bottom = LocalBottomBarPadding.current),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            SettingsCard {
                SettingsSection.entries.forEachIndexed { index, section ->
                    if (index > 0) RowDivider()
                    ListItem(
                        headlineContent = { Text(stringResource(section.title)) },
                        supportingContent = { Text(summary(section, preferences)) },
                        leadingContent = {
                            Icon(section.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { onOpenSection(section) },
                    )
                }
            }

            SettingsCard {
                LinkRow(
                    label = stringResource(R.string.settings_check_updates),
                    supporting = stringResource(R.string.settings_version, stringResource(R.string.app_name), BuildConfig.VERSION_NAME),
                    showChevron = false,
                    onClick = updateViewModel::check,
                )
            }
            UpdateDialogHost(updateViewModel)
        }
    }
}

@Composable
fun SettingsSectionScreen(
    section: SettingsSection,
    onBack: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenDuplicates: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val preferences by viewModel.preferences.collectAsState()
    val context = LocalContext.current
    var confirmReset by remember { mutableStateOf(false) }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.settings_reset_confirm)) },
            text = { Text(stringResource(R.string.settings_reset_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    viewModel.resetSettings()
                    context.toast(R.string.settings_reset_done)
                }) {
                    Text(stringResource(R.string.action_reset), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = { ScreenHeader(title = stringResource(section.title), onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = Spacing.md, end = Spacing.md, bottom = LocalBottomBarPadding.current + Spacing.md),
        ) {
            SettingsCard {
                when (section) {
                    SettingsSection.APPEARANCE -> {
                        ChoiceRow(
                            label = stringResource(R.string.settings_theme),
                            options = ThemeMode.entries,
                            selected = preferences.themeMode,
                            optionLabel = { stringResource(themeLabel(it)) },
                            onSelect = { viewModel.set(SettingsKeys.THEME_MODE, it.name) },
                        )
                        // Dynamic color needs Android 12.
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            RowDivider()
                            SwitchRow(
                                label = stringResource(R.string.settings_dynamic_color),
                                supporting = stringResource(R.string.settings_dynamic_color_body),
                                checked = preferences.dynamicColorEnabled,
                                onCheckedChange = { viewModel.set(SettingsKeys.DYNAMIC_COLOR, it) },
                            )
                        }
                    }

                    SettingsSection.LIBRARY -> {
                        ChoiceRow(
                            label = stringResource(R.string.settings_default_view),
                            options = listOf(false, true),
                            selected = preferences.timelineView,
                            optionLabel = { stringResource(if (it) R.string.library_timeline else R.string.library_grid) },
                            onSelect = { viewModel.set(SettingsKeys.TIMELINE_VIEW, it) },
                        )
                        RowDivider()
                        ChoiceRow(
                            label = stringResource(R.string.settings_grid_columns),
                            options = listOf(2, 3, 4),
                            selected = preferences.gridColumns,
                            optionLabel = { it.toString() },
                            onSelect = { viewModel.set(SettingsKeys.GRID_COLUMNS, it) },
                        )
                        RowDivider()
                        SwitchRow(
                            label = stringResource(R.string.settings_show_recent),
                            checked = preferences.showRecentOnHome,
                            onCheckedChange = { viewModel.set(SettingsKeys.SHOW_RECENT, it) },
                        )
                        RowDivider()
                        SwitchRow(
                            label = stringResource(R.string.settings_show_categories),
                            checked = preferences.showCategoriesOnHome,
                            onCheckedChange = { viewModel.set(SettingsKeys.SHOW_CATEGORIES, it) },
                        )
                    }

                    SettingsSection.SCANNING -> {
                        ChoiceRow(
                            label = stringResource(R.string.settings_auto_scan),
                            options = scanIntervals,
                            selected = preferences.autoScanHours,
                            optionLabel = { stringResource(scanLabel(it)) },
                            onSelect = { viewModel.set(SettingsKeys.AUTO_SCAN_HOURS, it) },
                        )
                        // Hidden rather than disabled: a greyed-out switch still asks to be read.
                        if (preferences.autoScanHours > 0) {
                            RowDivider()
                            SwitchRow(
                                label = stringResource(R.string.settings_charging_only),
                                checked = preferences.scanOnlyWhileCharging,
                                onCheckedChange = { viewModel.set(SettingsKeys.SCAN_ONLY_CHARGING, it) },
                            )
                        }
                    }

                    SettingsSection.PRIVACY -> {
                        SwitchRow(
                            label = stringResource(R.string.settings_secure),
                            supporting = stringResource(R.string.settings_secure_body),
                            checked = preferences.secureScreen,
                            onCheckedChange = { viewModel.set(SettingsKeys.SECURE_SCREEN, it) },
                        )
                        RowDivider()
                        SwitchRow(
                            label = stringResource(R.string.settings_analytics),
                            checked = preferences.analyticsEnabled,
                            onCheckedChange = { viewModel.set(SettingsKeys.ANALYTICS, it) },
                        )
                        RowDivider()
                        LinkRow(
                            label = stringResource(R.string.privacy_policy_title),
                            onClick = onOpenPrivacy,
                        )
                    }

                    SettingsSection.MAINTENANCE -> {
                        LinkRow(
                            label = stringResource(R.string.settings_duplicates),
                            onClick = onOpenDuplicates,
                        )
                        RowDivider()
                        LinkRow(
                            label = stringResource(R.string.settings_reindex),
                            supporting = stringResource(R.string.settings_reindex_body),
                            showChevron = false,
                            onClick = {
                                viewModel.rescanNow()
                                context.toast(R.string.settings_rescan_started)
                            },
                        )
                        RowDivider()
                        LinkRow(
                            label = stringResource(R.string.settings_reread),
                            supporting = stringResource(R.string.settings_reread_body),
                            showChevron = false,
                            onClick = {
                                viewModel.rereadAll()
                                context.toast(R.string.settings_reread_started)
                            },
                        )
                        RowDivider()
                        LinkRow(
                            label = stringResource(R.string.settings_clear_cache),
                            supporting = stringResource(R.string.settings_clear_cache_body),
                            showChevron = false,
                            onClick = {
                                viewModel.clearImageCache()
                                context.toast(R.string.settings_cache_cleared)
                            },
                        )
                        RowDivider()
                        LinkRow(
                            label = stringResource(R.string.settings_reset),
                            supporting = stringResource(R.string.settings_reset_body),
                            showChevron = false,
                            destructive = true,
                            onClick = { confirmReset = true },
                        )
                    }
                }
            }
        }
    }
}

private fun Context.toast(@StringRes message: Int) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    KitCard(modifier = Modifier.fillMaxWidth(), content = content)
}

@Composable
private fun RowDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.md), color = MaterialTheme.colorScheme.outlineVariant)
}

private fun supportingText(text: String?): (@Composable () -> Unit)? = text?.let { { Text(it) } }

@Composable
private fun LinkRow(
    label: String,
    onClick: () -> Unit,
    supporting: String? = null,
    showChevron: Boolean = true,
    destructive: Boolean = false,
) {
    ListItem(
        headlineContent = {
            Text(label, color = if (destructive) MaterialTheme.colorScheme.error else Color.Unspecified)
        },
        supportingContent = supportingText(supporting),
        trailingContent = if (showChevron) {
            { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }
        } else {
            null
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

/** Whole row toggles the switch, not only the 52dp thumb. */
@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    supporting: String? = null,
) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = supportingText(supporting),
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.secondary),
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.toggleable(
            value = checked,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ),
    )
}

/** Label with a segmented control under it, for settings with a few fixed choices. */
@Composable
private fun <T> ChoiceRow(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(Modifier.padding(Spacing.md)) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(Spacing.sm + Spacing.xs))
        SegmentedPills(options = options, selected = selected, label = optionLabel, onSelect = onSelect)
    }
}

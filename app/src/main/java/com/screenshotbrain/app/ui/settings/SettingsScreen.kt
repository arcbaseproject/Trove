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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ViewTimeline
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import com.trove.app.BuildConfig
import com.trove.app.R
import com.trove.app.data.settings.SettingsKeys
import com.trove.app.data.settings.ThemeMode
import com.trove.app.ui.components.KitCard
import com.trove.app.ui.components.ScreenHeader
import com.trove.app.ui.components.SegmentedPills
import com.trove.app.ui.theme.Spacing

private val scanIntervals = listOf(0, 1, 6, 24)

@Composable
fun SettingsScreen(
    onOpenPrivacy: () -> Unit,
    onOpenDuplicates: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val preferences by viewModel.preferences.collectAsState()
    val context = LocalContext.current

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
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            SectionLabel(stringResource(R.string.settings_appearance))
            SettingsCard {
                ChoiceRow(
                    icon = Icons.Outlined.DarkMode,
                    label = stringResource(R.string.settings_theme),
                    options = ThemeMode.entries,
                    selected = preferences.themeMode,
                    optionLabel = { mode ->
                        stringResource(
                            when (mode) {
                                ThemeMode.SYSTEM -> R.string.theme_system
                                ThemeMode.LIGHT -> R.string.theme_light
                                ThemeMode.DARK -> R.string.theme_dark
                            },
                        )
                    },
                    onSelect = { viewModel.set(SettingsKeys.THEME_MODE, it.name) },
                )
                // Dynamic color needs Android 12.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    RowDivider()
                    SwitchRow(
                        icon = Icons.Outlined.Palette,
                        label = stringResource(R.string.settings_dynamic_color),
                        supporting = stringResource(R.string.settings_dynamic_color_body),
                        checked = preferences.dynamicColorEnabled,
                        onCheckedChange = { viewModel.set(SettingsKeys.DYNAMIC_COLOR, it) },
                    )
                }
            }

            SectionLabel(stringResource(R.string.nav_library))
            SettingsCard {
                ChoiceRow(
                    icon = Icons.Outlined.GridView,
                    label = stringResource(R.string.settings_grid_columns),
                    options = listOf(2, 3, 4),
                    selected = preferences.gridColumns,
                    optionLabel = { it.toString() },
                    onSelect = { viewModel.set(SettingsKeys.GRID_COLUMNS, it) },
                )
                RowDivider()
                ChoiceRow(
                    icon = Icons.Outlined.ViewTimeline,
                    label = stringResource(R.string.settings_default_view),
                    options = listOf(false, true),
                    selected = preferences.timelineView,
                    optionLabel = { stringResource(if (it) R.string.library_timeline else R.string.library_grid) },
                    onSelect = { viewModel.set(SettingsKeys.TIMELINE_VIEW, it) },
                )
                RowDivider()
                SwitchRow(
                    icon = Icons.Outlined.History,
                    label = stringResource(R.string.settings_show_recent),
                    checked = preferences.showRecentOnHome,
                    onCheckedChange = { viewModel.set(SettingsKeys.SHOW_RECENT, it) },
                )
                RowDivider()
                SwitchRow(
                    icon = Icons.Outlined.Category,
                    label = stringResource(R.string.settings_show_categories),
                    checked = preferences.showCategoriesOnHome,
                    onCheckedChange = { viewModel.set(SettingsKeys.SHOW_CATEGORIES, it) },
                )
            }

            SectionLabel(stringResource(R.string.settings_scanning))
            SettingsCard {
                ChoiceRow(
                    icon = Icons.Outlined.Schedule,
                    label = stringResource(R.string.settings_auto_scan),
                    options = scanIntervals,
                    selected = preferences.autoScanHours,
                    optionLabel = { hours ->
                        stringResource(
                            when (hours) {
                                0 -> R.string.scan_off
                                1 -> R.string.scan_hourly
                                6 -> R.string.scan_6h
                                else -> R.string.scan_daily
                            },
                        )
                    },
                    onSelect = { viewModel.set(SettingsKeys.AUTO_SCAN_HOURS, it) },
                )
                RowDivider()
                SwitchRow(
                    icon = Icons.Outlined.BatteryChargingFull,
                    label = stringResource(R.string.settings_charging_only),
                    checked = preferences.scanOnlyWhileCharging,
                    enabled = preferences.autoScanHours > 0,
                    onCheckedChange = { viewModel.set(SettingsKeys.SCAN_ONLY_CHARGING, it) },
                )
                RowDivider()
                LinkRow(
                    icon = Icons.Outlined.Refresh,
                    label = stringResource(R.string.settings_reindex),
                    showChevron = false,
                    onClick = {
                        viewModel.rescanNow()
                        context.toast(R.string.settings_rescan_started)
                    },
                )
                RowDivider()
                LinkRow(
                    icon = Icons.Outlined.DocumentScanner,
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
                    icon = Icons.Outlined.ContentCopy,
                    label = stringResource(R.string.settings_duplicates),
                    onClick = onOpenDuplicates,
                )
            }

            SectionLabel(stringResource(R.string.privacy_title))
            SettingsCard {
                SwitchRow(
                    icon = Icons.Outlined.VisibilityOff,
                    label = stringResource(R.string.settings_secure),
                    supporting = stringResource(R.string.settings_secure_body),
                    checked = preferences.secureScreen,
                    onCheckedChange = { viewModel.set(SettingsKeys.SECURE_SCREEN, it) },
                )
                RowDivider()
                LinkRow(
                    icon = Icons.Outlined.Lock,
                    label = stringResource(R.string.privacy_policy_title),
                    onClick = onOpenPrivacy,
                )
                RowDivider()
                SwitchRow(
                    icon = Icons.Outlined.BarChart,
                    label = stringResource(R.string.settings_analytics),
                    checked = preferences.analyticsEnabled,
                    onCheckedChange = { viewModel.set(SettingsKeys.ANALYTICS, it) },
                )
            }

            SectionLabel(stringResource(R.string.settings_storage))
            SettingsCard {
                LinkRow(
                    icon = Icons.Outlined.CleaningServices,
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
                    icon = Icons.Outlined.RestartAlt,
                    label = stringResource(R.string.settings_reset),
                    supporting = stringResource(R.string.settings_reset_body),
                    showChevron = false,
                    onClick = {
                        viewModel.resetSettings()
                        context.toast(R.string.settings_reset_done)
                    },
                )
            }

            Text(
                text = stringResource(R.string.settings_version, stringResource(R.string.app_name), BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = Spacing.md),
            )
        }
    }
}

private fun Context.toast(@StringRes message: Int) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .padding(top = Spacing.sm, start = Spacing.xs)
            .semantics { heading() },
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    KitCard(modifier = Modifier.fillMaxWidth(), content = content)
}

@Composable
private fun RowDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.md), color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun RowIcon(icon: ImageVector) {
    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun supportingText(text: String?): (@Composable () -> Unit)? = text?.let { { Text(it) } }

@Composable
private fun LinkRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    supporting: String? = null,
    showChevron: Boolean = true,
) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = supportingText(supporting),
        leadingContent = { RowIcon(icon) },
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
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    supporting: String? = null,
    enabled: Boolean = true,
) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = supportingText(supporting),
        leadingContent = { RowIcon(icon) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.secondary),
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ),
    )
}

/** Label with a segmented control under it, for settings with a few fixed choices. */
@Composable
private fun <T> ChoiceRow(
    icon: ImageVector,
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(Modifier.padding(Spacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RowIcon(icon)
            Spacer(Modifier.width(Spacing.md))
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.height(Spacing.sm + Spacing.xs))
        SegmentedPills(options = options, selected = selected, label = optionLabel, onSelect = onSelect)
    }
}

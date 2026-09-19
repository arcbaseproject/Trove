@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.trove.app.ui.library

import com.trove.app.ui.navigation.LocalBottomBarPadding
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.trove.app.R
import com.trove.app.data.repository.DeleteRequestOutcome
import com.trove.app.domain.model.LibraryListItem
import com.trove.app.domain.model.ScreenshotUi
import com.trove.app.ui.components.EmptyState
import com.trove.app.ui.components.ScreenHeader
import com.trove.app.ui.components.ScreenshotThumbnail
import com.trove.app.ui.components.SegmentedPills
import com.trove.app.ui.components.ThumbnailGridSkeleton
import com.trove.app.ui.components.rememberDeleteGuard
import com.trove.app.ui.settings.LocalUserPreferences
import com.trove.app.ui.theme.Spacing
import com.trove.app.util.shareScreenshotUris
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(
    onBack: () -> Unit,
    onOpenScreenshot: (ScreenshotUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: LibraryViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val viewMode = if (LocalUserPreferences.current.timelineView) LibraryViewMode.TIMELINE else LibraryViewMode.GRID
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val deleteGuard = rememberDeleteGuard()

    BackHandler(enabled = uiState.selectionMode, onBack = viewModel::clearSelection)

    var pendingDeleteIds by remember { mutableStateOf<List<Long>>(emptyList()) }
    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.confirmDelete(pendingDeleteIds)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            if (uiState.selectionMode) {
                TopAppBar(
                    title = {
                        Text(pluralStringResource(R.plurals.selected_count, uiState.selection.size, uiState.selection.size))
                    },
                    navigationIcon = {
                        IconButton(onClick = viewModel::clearSelection) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_clear_selection))
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.toggleFavoriteForSelection() }) {
                            Icon(Icons.Filled.Favorite, contentDescription = stringResource(R.string.action_favorite))
                        }
                        IconButton(onClick = {
                            val ids = uiState.selection.toList()
                            scope.launch {
                                shareScreenshotUris(context, viewModel.contentUrisFor(ids))
                            }
                        }) {
                            Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.action_share))
                        }
                        IconButton(onClick = {
                            deleteGuard(uiState.selection.size) {
                                scope.launch {
                                    when (val outcome = viewModel.requestDeleteSelection()) {
                                        is DeleteRequestOutcome.NeedsConfirmation -> {
                                            pendingDeleteIds = outcome.ids
                                            deleteLauncher.launch(IntentSenderRequest.Builder(outcome.pendingIntent).build())
                                        }
                                        is DeleteRequestOutcome.Deleted -> Unit
                                    }
                                }
                            }
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
            } else {
                // The Library tab is a root screen: back only when opened for a category.
                ScreenHeader(
                    title = uiState.categoryTitle ?: stringResource(R.string.nav_library),
                    onBack = if (uiState.categoryTitle != null) onBack else null,
                ) {
                    Spacer(Modifier.height(Spacing.md))
                    SegmentedPills(
                        options = LibraryViewMode.entries,
                        selected = viewMode,
                        label = { mode ->
                            stringResource(if (mode == LibraryViewMode.GRID) R.string.library_grid else R.string.library_timeline)
                        },
                        onSelect = viewModel::setViewMode,
                    )
                }
            }
        },
    ) { padding ->
        when (viewMode) {
            LibraryViewMode.GRID -> GridContent(
                viewModel = viewModel,
                selection = uiState.selection,
                selectionMode = uiState.selectionMode,
                onOpenScreenshot = onOpenScreenshot,
                modifier = Modifier.padding(padding),
            )
            LibraryViewMode.TIMELINE -> TimelineContent(
                viewModel = viewModel,
                selection = uiState.selection,
                selectionMode = uiState.selectionMode,
                onOpenScreenshot = onOpenScreenshot,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun GridContent(
    viewModel: LibraryViewModel,
    selection: Set<Long>,
    selectionMode: Boolean,
    onOpenScreenshot: (ScreenshotUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = viewModel.pagingData.collectAsLazyPagingItems()
    if (items.itemCount == 0) {
        LibraryEmptyState(items, modifier)
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(LocalUserPreferences.current.gridColumns),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, bottom = Spacing.md + LocalBottomBarPadding.current),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(count = items.itemCount, key = items.itemKey { it.id }) { index ->
            val screenshot = items[index]
            if (screenshot != null) {
                ScreenshotThumbnail(
                    screenshot = screenshot,
                    isSelected = screenshot.id in selection,
                    selectionMode = selectionMode,
                    modifier = Modifier.combinedClickable(
                        onClick = { if (selectionMode) viewModel.toggleSelection(screenshot.id) else onOpenScreenshot(screenshot) },
                        onLongClick = { viewModel.toggleSelection(screenshot.id) },
                    ),
                )
            }
        }
    }
}

@Composable
private fun TimelineContent(
    viewModel: LibraryViewModel,
    selection: Set<Long>,
    selectionMode: Boolean,
    onOpenScreenshot: (ScreenshotUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = viewModel.timelinePagingData.collectAsLazyPagingItems()
    if (items.itemCount == 0) {
        LibraryEmptyState(items, modifier)
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(LocalUserPreferences.current.gridColumns),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, bottom = Spacing.md + LocalBottomBarPadding.current),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(
            count = items.itemCount,
            key = items.itemKey { entry -> if (entry is LibraryListItem.Item) entry.screenshot.id else entry.hashCode() },
            span = { index -> if (items[index] is LibraryListItem.Header) GridItemSpan(maxLineSpan) else GridItemSpan(1) },
        ) { index ->
            when (val entry = items[index]) {
                is LibraryListItem.Header -> TimelineHeader(entry.label)
                is LibraryListItem.Item -> ScreenshotThumbnail(
                    screenshot = entry.screenshot,
                    isSelected = entry.screenshot.id in selection,
                    selectionMode = selectionMode,
                    modifier = Modifier.combinedClickable(
                        onClick = {
                            if (selectionMode) viewModel.toggleSelection(entry.screenshot.id) else onOpenScreenshot(entry.screenshot)
                        },
                        onLongClick = { viewModel.toggleSelection(entry.screenshot.id) },
                    ),
                )
                null -> Unit
            }
        }
    }
}

@Composable
private fun LazyGridItemScope.TimelineHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.xs),
    )
}

/** Skeleton grid until the first page lands, so the empty message doesn't flash while loading. */
@Composable
private fun LibraryEmptyState(items: LazyPagingItems<*>, modifier: Modifier) {
    if (items.loadState.refresh !is LoadState.NotLoading) {
        ThumbnailGridSkeleton(
            columns = LocalUserPreferences.current.gridColumns,
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, bottom = Spacing.md + LocalBottomBarPadding.current),
        )
        return
    }
    EmptyState(
        title = stringResource(R.string.empty_library_title),
        body = stringResource(R.string.empty_library_body),
        modifier = modifier.fillMaxSize(),
    )
}

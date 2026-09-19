package com.trove.app.ui.collections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.trove.app.R
import com.trove.app.domain.model.ScreenshotUi
import com.trove.app.ui.components.EmptyState
import com.trove.app.ui.components.ScreenHeader
import com.trove.app.ui.components.ScreenshotThumbnail
import com.trove.app.ui.components.ThumbnailGridSkeleton
import com.trove.app.ui.settings.LocalUserPreferences
import com.trove.app.ui.theme.Spacing

@Composable
fun CollectionDetailScreen(
    onBack: () -> Unit,
    onOpenScreenshot: (ScreenshotUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: CollectionDetailViewModel = hiltViewModel()
    val items = viewModel.pagingData.collectAsLazyPagingItems()

    Scaffold(
        modifier = modifier,
        topBar = { ScreenHeader(title = viewModel.collectionName, onBack = onBack) },
    ) { padding ->
        if (items.itemCount == 0) {
            if (items.loadState.refresh is LoadState.NotLoading) {
                EmptyState(
                    title = stringResource(R.string.empty_collection_title),
                    body = stringResource(R.string.empty_collection_body),
                    modifier = Modifier.padding(padding).fillMaxSize(),
                )
            } else {
                ThumbnailGridSkeleton(
                    columns = LocalUserPreferences.current.gridColumns,
                    modifier = Modifier.padding(padding).fillMaxSize(),
                )
            }
            return@Scaffold
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(LocalUserPreferences.current.gridColumns),
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, bottom = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            items(count = items.itemCount, key = items.itemKey { it.id }) { index ->
                val screenshot = items[index]
                if (screenshot != null) {
                    ScreenshotThumbnail(
                        screenshot = screenshot,
                        modifier = Modifier.clickable { onOpenScreenshot(screenshot) },
                    )
                }
            }
        }
    }
}

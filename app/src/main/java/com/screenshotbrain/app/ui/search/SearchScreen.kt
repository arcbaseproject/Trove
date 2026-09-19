package com.trove.app.ui.search

import com.trove.app.ui.navigation.LocalBottomBarPadding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.trove.app.R
import com.trove.app.domain.model.ScreenshotUi
import com.trove.app.ui.components.EmptyState
import com.trove.app.ui.components.ScreenHeader
import com.trove.app.ui.components.ScreenshotSearchField
import com.trove.app.ui.components.ScreenshotThumbnail
import com.trove.app.ui.components.ThumbnailGridSkeleton
import com.trove.app.ui.settings.LocalUserPreferences
import com.trove.app.ui.theme.Spacing

@Composable
fun SearchScreen(
    onOpenScreenshot: (String, ScreenshotUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: SearchViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(title = stringResource(R.string.nav_search)) {
            Spacer(Modifier.height(Spacing.md))
            ScreenshotSearchField(query = uiState.query, onQueryChange = viewModel::onQueryChange)
        }

        when {
            uiState.query.isBlank() -> EmptyState(
                title = stringResource(R.string.search_prompt_title),
                body = stringResource(R.string.search_prompt_body),
                icon = Icons.Outlined.Search,
                modifier = Modifier.fillMaxSize(),
            )
            uiState.isSearching -> ThumbnailGridSkeleton(
                columns = LocalUserPreferences.current.gridColumns,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, bottom = Spacing.md + LocalBottomBarPadding.current),
            )
            uiState.results.isEmpty() && uiState.hasSearched -> EmptyState(
                title = stringResource(R.string.empty_search_title),
                body = stringResource(R.string.empty_search_body),
                icon = Icons.Outlined.SearchOff,
                modifier = Modifier.fillMaxSize(),
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(LocalUserPreferences.current.gridColumns),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, bottom = Spacing.md + LocalBottomBarPadding.current),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(uiState.results, key = { it.id }) { screenshot ->
                    ScreenshotThumbnail(
                        screenshot = screenshot,
                        modifier = Modifier.clickable { onOpenScreenshot(uiState.query, screenshot) },
                    )
                }
            }
        }
    }
}

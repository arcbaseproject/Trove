package com.trove.app.ui.home

import com.trove.app.ui.navigation.LocalBottomBarPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.trove.app.R
import com.trove.app.data.local.db.dao.CategoryWithCount
import com.trove.app.domain.model.ScreenshotUi
import com.trove.app.ui.components.CardGridSkeleton
import com.trove.app.ui.components.EmptyState
import com.trove.app.ui.components.ThumbnailRowSkeleton
import com.trove.app.ui.components.IndexingBanner
import com.trove.app.ui.components.KitCard
import com.trove.app.ui.components.ScreenHeader
import com.trove.app.ui.components.ScreenshotThumbnail
import com.trove.app.ui.components.SectionHeader
import com.trove.app.ui.components.TagPill
import com.trove.app.ui.components.categoryIcon
import com.trove.app.ui.settings.LocalUserPreferences
import com.trove.app.ui.theme.Spacing

@Composable
fun HomeScreen(
    onOpenSearch: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenCategory: (String) -> Unit,
    onOpenScreenshot: (ScreenshotUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: HomeViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val preferences = LocalUserPreferences.current

    val header = @Composable {
        ScreenHeader(
            title = stringResource(R.string.app_name),
            subtitle = pluralStringResource(R.plurals.screenshots_saved, uiState.totalCount, uiState.totalCount),
        ) {
            Spacer(Modifier.height(Spacing.md))
            SearchEntryField(onClick = onOpenSearch)
        }
    }

    if (uiState.isLoading) {
        Column(modifier.fillMaxSize()) {
            header()
            Spacer(Modifier.height(Spacing.sm))
            ThumbnailRowSkeleton()
            Spacer(Modifier.height(Spacing.lg))
            CardGridSkeleton()
        }
        return
    }

    if (uiState.totalCount == 0 && !uiState.indexingProgress.isRunning) {
        Column(modifier.fillMaxSize()) {
            header()
            EmptyState(
                title = stringResource(R.string.empty_library_title),
                body = stringResource(R.string.empty_library_body),
                modifier = Modifier.weight(1f),
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Spacing.lg + LocalBottomBarPadding.current),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item { header() }

        item { IndexingBanner(progress = uiState.indexingProgress) }

        if (preferences.showRecentOnHome && uiState.recentScreenshots.isNotEmpty()) {
            item {
                SectionHeader(title = stringResource(R.string.recent_screenshots), onSeeAll = onOpenLibrary)
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm + Spacing.xs),
                ) {
                    items(uiState.recentScreenshots, key = { it.id }) { screenshot ->
                        ScreenshotThumbnail(
                            screenshot = screenshot,
                            modifier = Modifier
                                .width(116.dp)
                                .clickable { onOpenScreenshot(screenshot) },
                        )
                    }
                }
            }
        }

        if (preferences.showCategoriesOnHome && uiState.categories.isNotEmpty()) {
            item {
                Spacer(Modifier.height(Spacing.sm))
                SectionHeader(title = stringResource(R.string.smart_collections))
            }
            items(uiState.categories.chunked(2)) { row ->
                Row(
                    modifier = Modifier.padding(horizontal = Spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm + Spacing.xs),
                ) {
                    row.forEach { category ->
                        CategoryCard(
                            category = category,
                            onClick = { onOpenCategory(category.categoryKey) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SearchEntryField(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(Spacing.sm))
        Text(
            stringResource(R.string.search_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CategoryCard(category: CategoryWithCount, onClick: () -> Unit, modifier: Modifier = Modifier) {
    KitCard(onClick = onClick, modifier = modifier) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    categoryIcon(category.categoryKey),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(Spacing.sm + Spacing.xs))
            Text(
                category.displayName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(Spacing.xs))
            TagPill(pluralStringResource(R.plurals.screenshot_count, category.count, category.count))
        }
    }
}

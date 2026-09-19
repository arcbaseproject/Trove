package com.trove.app.ui.onboarding

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.trove.app.R
import com.trove.app.ui.components.KitCard
import com.trove.app.ui.components.TagPill
import com.trove.app.ui.components.categoryIcon
import com.trove.app.ui.theme.Spacing
import kotlinx.coroutines.launch

private class Page(@StringRes val title: Int, @StringRes val body: Int, val art: @Composable () -> Unit)

private val pages = listOf(
    Page(R.string.onboarding_sorted_title, R.string.onboarding_sorted_body) { SortedArt() },
    Page(R.string.onboarding_search_title, R.string.onboarding_search_body) { SearchArt() },
    Page(R.string.onboarding_private_title, R.string.onboarding_private_body) { PrivateArt() },
)

/** Three intro pages. [onFinish] runs from "Get started" or "Skip"; the caller asks for photo access next. */
@Composable
fun OnboardingScreen(onFinish: () -> Unit, modifier: Modifier = Modifier) {
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == pages.lastIndex

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), horizontalArrangement = Arrangement.End) {
            if (!isLast) {
                TextButton(onClick = onFinish) { Text(stringResource(R.string.onboarding_skip)) }
            }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { index ->
            val page = pages[index]
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clearAndSetSemantics {},
                    contentAlignment = Alignment.Center,
                ) { page.art() }
                Text(
                    stringResource(page.title),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    stringResource(page.body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = Spacing.md),
                )
                Spacer(Modifier.height(Spacing.xl))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PageDots(count = pages.size, current = pagerState.currentPage, modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    if (isLast) onFinish() else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                modifier = Modifier.height(52.dp),
            ) {
                Text(stringResource(if (isLast) R.string.onboarding_start else R.string.onboarding_next))
            }
        }
    }
}

@Composable
private fun PageDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier.clearAndSetSemantics {}, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { index ->
            val width by animateDpAsState(if (index == current) 24.dp else 8.dp, label = "dot")
            Box(
                Modifier
                    .size(width = width, height = 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    ),
            )
        }
    }
}

/** Fanned-out category cards, like the Home screen's. */
@Composable
private fun SortedArt() {
    val cards = listOf(
        Triple("RECEIPTS", R.string.onboarding_art_receipts, -6f),
        Triple("CODE", R.string.onboarding_art_code, 4f),
        Triple("CONVERSATIONS", R.string.onboarding_art_chats, -2f),
    )
    Column(verticalArrangement = Arrangement.spacedBy((-12).dp)) {
        cards.forEachIndexed { index, (key, label, angle) ->
            KitCard(
                Modifier
                    .width(240.dp)
                    .padding(start = (index * 20).dp)
                    .rotate(angle),
            ) {
                Row(Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
                    IconTile { Icon(categoryIcon(key), contentDescription = null) }
                    Spacer(Modifier.width(Spacing.sm + Spacing.xs))
                    Column {
                        Text(stringResource(label), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(Spacing.xs))
                        TagPill(stringResource(R.string.onboarding_art_count, 12 + index * 17))
                    }
                }
            }
        }
    }
}

/** A search bar over a result with the matched words highlighted. */
@Composable
private fun SearchArt() {
    Column(Modifier.width(264.dp), verticalArrangement = Arrangement.spacedBy(Spacing.sm + Spacing.xs)) {
        KitCard(shape = CircleShape) {
            Row(Modifier.padding(horizontal = Spacing.md, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(Spacing.sm))
                Text(stringResource(R.string.onboarding_art_query), style = MaterialTheme.typography.bodyLarge)
            }
        }
        KitCard {
            Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                FakeLine(0.6f)
                TagPill(stringResource(R.string.onboarding_art_match))
                FakeLine(0.85f)
                FakeLine(0.45f)
            }
        }
    }
}

/** Lock with the three privacy promises. */
@Composable
private fun PrivateArt() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Lock,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Spacer(Modifier.height(Spacing.lg))
        KitCard(Modifier.width(264.dp)) {
            Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm + Spacing.xs)) {
                listOf(R.string.onboarding_art_on_device, R.string.onboarding_art_no_account, R.string.onboarding_art_no_uploads)
                    .forEach { line ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(Spacing.sm))
                            Text(stringResource(line), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
            }
        }
    }
}

@Composable
private fun IconTile(content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun FakeLine(fraction: Float) {
    Box(
        Modifier
            .fillMaxWidth(fraction)
            .height(10.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

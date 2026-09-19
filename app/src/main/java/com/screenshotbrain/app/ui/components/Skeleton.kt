package com.trove.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.trove.app.R
import com.trove.app.ui.theme.Spacing

/** Pulsing placeholder block. Alpha pulse, not a gradient sweep: one animated value, no per-frame brush. */
@Composable
fun SkeletonBox(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(16.dp)) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
        label = "skeletonAlpha",
    )
    Box(
        modifier
            .clip(shape)
            .alpha(alpha)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

/** Stand-in for a thumbnail grid while the first page loads. Matches the real grid's spacing. */
@Composable
fun ThumbnailGridSkeleton(
    columns: Int,
    modifier: Modifier = Modifier,
    count: Int = columns * 4,
    contentPadding: PaddingValues = PaddingValues(Spacing.md),
) {
    val loading = stringResource(R.string.loading)
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.semantics { contentDescription = loading },
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        userScrollEnabled = false,
    ) {
        items(count) { SkeletonBox(Modifier.aspectRatio(9f / 16f)) }
    }
}

/** Horizontal strip of thumbnail placeholders, sized like the home screen's recents row. */
@Composable
fun ThumbnailRowSkeleton(modifier: Modifier = Modifier, count: Int = 4) {
    Row(
        modifier = modifier.padding(horizontal = Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm + Spacing.xs),
    ) {
        repeat(count) { SkeletonBox(Modifier.width(116.dp).aspectRatio(9f / 16f)) }
    }
}

/** Two-up card placeholders, sized like the home categories / collections cards. */
@Composable
fun CardGridSkeleton(modifier: Modifier = Modifier, rows: Int = 2, cardHeight: Dp = 140.dp) {
    val loading = stringResource(R.string.loading)
    Column(
        modifier = modifier
            .padding(horizontal = Spacing.md)
            .semantics { contentDescription = loading },
        verticalArrangement = Arrangement.spacedBy(Spacing.sm + Spacing.xs),
    ) {
        repeat(rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm + Spacing.xs)) {
                repeat(2) { SkeletonBox(Modifier.weight(1f).height(cardHeight), CardShape) }
            }
        }
    }
}

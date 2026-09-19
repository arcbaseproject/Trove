package com.trove.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.trove.app.R
import com.trove.app.domain.model.IndexingProgress
import com.trove.app.ui.theme.Spacing

/** Card with a periwinkle progress bar, shown while background indexing runs. */
@Composable
fun IndexingBanner(progress: IndexingProgress, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = progress.isRunning && !progress.isComplete) {
        KitCard(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md),
        ) {
            Row(modifier = Modifier.padding(start = Spacing.md, end = Spacing.md, top = Spacing.md)) {
                Text(
                    stringResource(R.string.indexing_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(R.string.indexing_count, progress.processed, progress.total),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(Spacing.sm))
            LinearProgressIndicator(
                progress = { if (progress.total > 0) progress.processed / progress.total.toFloat() else 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Spacing.md, end = Spacing.md, bottom = Spacing.md)
                    .height(8.dp),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
                drawStopIndicator = {},
            )
        }
    }
}

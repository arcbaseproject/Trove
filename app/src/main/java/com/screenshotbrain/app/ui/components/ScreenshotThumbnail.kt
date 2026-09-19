package com.trove.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.trove.app.R
import com.trove.app.domain.model.ScreenshotUi
import com.trove.app.ui.theme.ScreenshotBrainTheme
import com.trove.app.util.formatDateTime

/**
 * A single grid cell. Coil handles thumbnail decoding/caching, so this stays a thin wrapper —
 * the important bit is a stable content URI key so Compose/Coil don't redecode on recomposition.
 */
@Composable
fun ScreenshotThumbnail(
    screenshot: ScreenshotUi,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    selectionMode: Boolean = false,
) {
    val shape = RoundedCornerShape(16.dp)
    val label = stringResource(R.string.screenshot_label, formatDateTime(screenshot.dateAddedSec))
    // Clip first so the caller's click ripple follows the rounded corners.
    Box(
        modifier = Modifier
            .clip(shape)
            .then(modifier)
            .aspectRatio(9f / 16f)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape) else Modifier)
            .semantics {
                contentDescription = label
                if (selectionMode) selected = isSelected
            },
    ) {
        AsyncImage(
            model = screenshot.contentUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (screenshot.isFavorite && !selectionMode) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(16.dp),
            )
        }
        if (selectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.35f)),
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun ScreenshotThumbnailPreview() {
    ScreenshotBrainTheme {
        ScreenshotThumbnail(
            screenshot = ScreenshotUi(1, "", "", 0, 1080, 1920, isFavorite = true),
            selectionMode = true,
            isSelected = true,
        )
    }
}

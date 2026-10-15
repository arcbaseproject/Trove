package com.trove.app.ui.update

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.trove.app.BuildConfig
import com.trove.app.R
import com.trove.app.ui.components.TagPill
import com.trove.app.ui.theme.Spacing

/**
 * Checks GitHub once per launch and offers the new APK. Everything is opt-out by dismissal —
 * the check is a single HTTPS GET to the releases API, nothing about the library is sent.
 * Stays silent when a store (Obtainium, Orion Store, …) installed Trove, since that store updates it.
 */
@Composable
fun UpdatePrompt(viewModel: UpdateViewModel = hiltViewModel()) {
    LaunchedEffect(Unit) { viewModel.checkOnLaunch() }
    UpdateDialogHost(viewModel)
}

/** The sheet alone, for callers that trigger the check themselves (Settings). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateDialogHost(viewModel: UpdateViewModel) {
    val state by viewModel.uiState.collectAsState()
    val release = state.release ?: return
    // A half-finished download can't be cancelled from here, so the sheet stays put while it runs.
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { !state.isDownloading },
    )
    ModalBottomSheet(
        onDismissRequest = { if (!state.isDownloading) viewModel.dismiss() },
        sheetState = sheetState,
    ) {
        Column(Modifier.padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon()
                Spacer(Modifier.width(Spacing.md))
                Column {
                    TagPill(
                        stringResource(R.string.update_badge),
                        container = MaterialTheme.colorScheme.tertiaryContainer,
                        content = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(stringResource(R.string.update_title, release.version), style = MaterialTheme.typography.headlineSmall)
                    Text(
                        stringResource(R.string.update_current, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            val notes = noteLines(release.notes)
            if (notes.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.lg))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(Spacing.md),
                ) {
                    Text(
                        stringResource(R.string.update_whats_new),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Column(
                        modifier = Modifier
                            .heightIn(max = 220.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        notes.forEach { NoteLine(it) }
                    }
                }
            }

            Spacer(Modifier.height(Spacing.lg))
            AnimatedContent(targetState = state.isDownloading, label = "update action") { downloading ->
                if (downloading) {
                    DownloadProgress(state.progress)
                } else {
                    Column {
                        Button(
                            onClick = viewModel::download,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        ) {
                            Text(stringResource(R.string.update_install), style = MaterialTheme.typography.titleMedium)
                        }
                        TextButton(onClick = viewModel::dismiss, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.update_later), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

/** The launcher icon as a tile, so the sheet reads as "this app" at a glance. */
@Composable
private fun AppIcon() {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(colorResource(R.color.ic_launcher_background)),
    ) {
        // Adaptive foregrounds carry a wide safe-zone margin; scale it up to fill the tile.
        Image(
            painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.scale(1.5f),
        )
    }
}

@Composable
private fun NoteLine(line: NoteLineUi) {
    if (line.heading) {
        Text(line.text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        return
    }
    Row {
        Box(
            Modifier
                .padding(top = 8.dp, end = Spacing.sm)
                .size(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary),
        )
        Text(line.text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DownloadProgress(progress: Float?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp + 48.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        if (progress == null) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.secondary,
            )
        } else {
            val animated by animateFloatAsState(progress, label = "download progress")
            LinearProgressIndicator(
                progress = { animated },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        Text(
            stringResource(R.string.update_downloading, ((progress ?: 0f) * 100).toInt()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

internal data class NoteLineUi(val text: String, val heading: Boolean = false)

/** GitHub release notes are Markdown; show headings and bullets, drop the rest of the syntax. */
internal fun noteLines(notes: String): List<NoteLineUi> = notes.lines()
    .map { it.trim() }
    .filter { it.isNotEmpty() }
    .map { line ->
        when {
            line.startsWith("#") -> NoteLineUi(line.trimStart('#').trim(), heading = true)
            line.startsWith("- ") || line.startsWith("* ") -> NoteLineUi(line.drop(2).trim())
            else -> NoteLineUi(line)
        }
    }
    .map { it.copy(text = it.text.replace("**", "").replace("`", "")) }

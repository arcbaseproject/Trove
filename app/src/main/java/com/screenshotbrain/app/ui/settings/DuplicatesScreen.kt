package com.trove.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.trove.app.R
import com.trove.app.data.repository.DeleteRequestOutcome
import com.trove.app.ui.components.EmptyState
import com.trove.app.ui.components.KitCard
import com.trove.app.ui.components.TagPill
import com.trove.app.ui.components.ScreenHeader
import com.trove.app.ui.components.ScreenshotThumbnail
import com.trove.app.ui.components.rememberDeleteGuard
import com.trove.app.ui.theme.Spacing
import kotlinx.coroutines.launch

@Composable
fun DuplicatesScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel: DuplicatesViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val deleteGuard = rememberDeleteGuard()

    var pendingDeleteGroup by remember { mutableStateOf<DuplicateGroupUi?>(null) }
    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && pendingDeleteGroup != null) {
            viewModel.refresh()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = { ScreenHeader(title = stringResource(R.string.duplicates_title), onBack = onBack) },
    ) { padding ->
        when {
            uiState.isLoading -> Column(
                modifier = Modifier.padding(padding).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator() }

            uiState.groups.isEmpty() -> EmptyState(
                title = stringResource(R.string.empty_duplicates_title),
                body = stringResource(R.string.empty_duplicates_body),
                modifier = Modifier.padding(padding).fillMaxSize(),
            )

            else -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, bottom = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                items(uiState.groups) { group ->
                    DuplicateGroupCard(
                        group = group,
                        onDeleteExtras = {
                            deleteGuard(group.screenshots.size - 1) {
                                scope.launch {
                                    when (val outcome = viewModel.requestDeleteAllButFirst(group)) {
                                        is DeleteRequestOutcome.NeedsConfirmation -> {
                                            pendingDeleteGroup = group
                                            deleteLauncher.launch(IntentSenderRequest.Builder(outcome.pendingIntent).build())
                                        }
                                        is DeleteRequestOutcome.Deleted -> viewModel.refresh()
                                    }
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DuplicateGroupCard(group: DuplicateGroupUi, onDeleteExtras: () -> Unit) {
    KitCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                pluralStringResource(R.plurals.duplicate_group_title, group.screenshots.size, group.screenshots.size),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(Spacing.sm))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                itemsIndexed(group.screenshots, key = { _, it -> it.id }) { index, screenshot ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ScreenshotThumbnail(screenshot = screenshot, modifier = Modifier.width(90.dp))
                        Spacer(Modifier.height(Spacing.xs))
                        // The list is newest first, and the newest one survives.
                        if (index == 0) {
                            TagPill(stringResource(R.string.duplicate_keep))
                        } else {
                            TagPill(
                                stringResource(R.string.action_delete),
                                container = MaterialTheme.colorScheme.tertiaryContainer,
                                content = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(Spacing.md))
            Row {
                Button(onClick = onDeleteExtras) {
                    val extras = group.screenshots.size - 1
                    Text(pluralStringResource(R.plurals.duplicate_delete_action, extras, extras))
                }
            }
        }
    }
}

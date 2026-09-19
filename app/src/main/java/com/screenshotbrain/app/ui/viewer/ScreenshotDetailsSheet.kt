package com.trove.app.ui.viewer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.trove.app.R
import com.trove.app.domain.model.CategoryRefUi
import com.trove.app.domain.model.CollectionUi
import com.trove.app.domain.model.ScreenshotDetail
import com.trove.app.ui.theme.Spacing
import com.trove.app.util.formatDateTime
import com.trove.app.util.ocrActions
import com.trove.app.util.formatFileSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenshotDetailsSheet(
    detail: ScreenshotDetail?,
    allCollections: List<CollectionUi>,
    onDismiss: () -> Unit,
    onNotesChanged: (String) -> Unit,
    onAddTag: (String) -> Unit,
    onRemoveTag: (Long) -> Unit,
    onAddToCollection: (Long) -> Unit,
    onRemoveFromCollection: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        if (detail == null) return@ModalBottomSheet
        val clipboard = LocalClipboardManager.current
        val context = LocalContext.current
        var notes by remember(detail.screenshot.id) { mutableStateOf(detail.notes.orEmpty()) }
        var newTag by remember(detail.screenshot.id) { mutableStateOf("") }
        val submitTag = {
            if (newTag.isNotBlank()) {
                onAddTag(newTag.trim())
                newTag = ""
            }
        }

        Column(modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)) {
            Text(stringResource(R.string.details_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(Spacing.md))

            DetailRow(stringResource(R.string.details_date), formatDateTime(detail.screenshot.dateAddedSec))
            DetailRow(stringResource(R.string.details_dimensions), "${detail.screenshot.width} × ${detail.screenshot.height}")
            DetailRow(stringResource(R.string.details_size), formatFileSize(detail.sizeBytes))
            detail.sourceAppLabel?.let { DetailRow(stringResource(R.string.details_source_app), it) }

            if (detail.categories.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.md))
                Text(stringResource(R.string.details_categories), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(Spacing.xs))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    items(detail.categories, key = { it.key }) { category ->
                        CategoryBadge(category)
                    }
                }
            }

            Spacer(Modifier.height(Spacing.md))
            Text(stringResource(R.string.details_tags), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Spacing.xs))
            if (detail.tags.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    items(detail.tags, key = { it.id }) { tag ->
                        InputChip(
                            selected = false,
                            onClick = { onRemoveTag(tag.id) },
                            label = { Text(tag.name) },
                            trailingIcon = {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.action_remove_tag, tag.name),
                                    modifier = Modifier.size(InputChipDefaults.IconSize),
                                )
                            },
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.xs))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newTag,
                    onValueChange = { newTag = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(stringResource(R.string.action_add_tag)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submitTag() }),
                )
                IconButton(onClick = submitTag) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_add_tag))
                }
            }

            if (allCollections.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.md))
                Text(stringResource(R.string.details_collections), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(Spacing.xs))
                val memberIds = detail.collections.map { it.id }.toSet()
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    items(allCollections, key = { it.id }) { collection ->
                        val isMember = collection.id in memberIds
                        // FilterChip adds a checkmark, so membership isn't shown by color alone.
                        FilterChip(
                            selected = isMember,
                            onClick = {
                                if (isMember) onRemoveFromCollection(collection.id) else onAddToCollection(collection.id)
                            },
                            label = { Text(collection.name) },
                            leadingIcon = if (isMember) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else {
                                null
                            },
                        )
                    }
                }
            }

            val actions = remember(detail.ocrText) { detail.ocrText?.let(::ocrActions).orEmpty() }
            if (actions.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.md))
                Text(stringResource(R.string.details_actions), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(Spacing.xs))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    items(actions, key = { it.label }) { action ->
                        AssistChip(
                            onClick = { runCatching { context.startActivity(action.intent) } },
                            label = { Text(action.label, maxLines = 1) },
                        )
                    }
                }
            }

            if (!detail.ocrText.isNullOrBlank()) {
                Spacer(Modifier.height(Spacing.md))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.details_ocr_text), style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { clipboard.setText(AnnotatedString(detail.ocrText)) }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.action_copy_text))
                    }
                }
                Text(detail.ocrText, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(Spacing.md))
            OutlinedTextField(
                value = notes,
                onValueChange = {
                    notes = it
                    onNotesChanged(it)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.details_notes)) },
                minLines = 2,
            )
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}

@Composable
private fun CategoryBadge(category: CategoryRefUi) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = category.displayName,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

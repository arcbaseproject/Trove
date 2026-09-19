package com.trove.app.ui.collections

import com.trove.app.ui.navigation.LocalBottomBarPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.trove.app.R
import com.trove.app.domain.model.CollectionUi
import com.trove.app.ui.components.CardGridSkeleton
import com.trove.app.ui.components.EmptyState
import com.trove.app.ui.components.KitCard
import com.trove.app.ui.components.ScreenHeader
import com.trove.app.ui.components.TagPill
import com.trove.app.ui.theme.Spacing

@Composable
fun CollectionsScreen(
    onOpenCollection: (CollectionUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: CollectionsViewModel = hiltViewModel()
    // .value, not `by`: delegated properties don't smart-cast after the null check below.
    val collections = viewModel.collections.collectAsState().value
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            ScreenHeader(
                title = stringResource(R.string.nav_collections),
                actions = {
                    Button(onClick = { showCreateDialog = true }, contentPadding = PaddingValues(start = 12.dp, end = 16.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(Spacing.xs))
                        Text(stringResource(R.string.action_new))
                    }
                },
            )
        },
    ) { padding ->
        if (collections == null) {
            CardGridSkeleton(modifier = Modifier.padding(padding).fillMaxSize(), cardHeight = 220.dp)
        } else if (collections.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.empty_collections_title),
                body = stringResource(R.string.empty_collections_body),
                icon = Icons.Outlined.Folder,
                modifier = Modifier.padding(padding).fillMaxSize(),
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, bottom = Spacing.md + LocalBottomBarPadding.current),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm + Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm + Spacing.xs),
            ) {
                items(collections, key = { it.id }) { collection ->
                    CollectionCard(collection = collection, onClick = { onOpenCollection(collection) })
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateCollectionDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                viewModel.createCollection(name)
                showCreateDialog = false
            },
        )
    }
}

@Composable
private fun CollectionCard(collection: CollectionUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    KitCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        // Empty collections get a blank cover so every card in a row has the same height.
        AsyncImage(
            model = collection.coverUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .padding(Spacing.sm)
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Column(modifier = Modifier.padding(start = Spacing.md, end = Spacing.md, bottom = Spacing.md)) {
            Text(collection.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(Spacing.xs))
            TagPill(pluralStringResource(R.plurals.screenshot_count, collection.count, collection.count))
        }
    }
}

@Composable
private fun CreateCollectionDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_new_collection)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(stringResource(R.string.collection_name)) },
                modifier = Modifier.focusRequester(focusRequester),
            )
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name.trim()) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.action_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

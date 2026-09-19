package com.trove.app.ui.viewer

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.trove.app.R
import com.trove.app.data.repository.DeleteRequestOutcome
import com.trove.app.ui.components.rememberDeleteGuard
import com.trove.app.util.shareScreenshotUris
import kotlinx.coroutines.launch

@Composable
fun ScreenshotViewerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ScreenshotViewerViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val deleteGuard = rememberDeleteGuard()

    var pendingDeleteIds by remember { mutableStateOf<List<Long>>(emptyList()) }
    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.confirmDelete(pendingDeleteIds)
            if (pendingDeleteIds.isNotEmpty()) onBack()
        }
    }

    // Black viewer: light status bar icons while it's open, then restore.
    val view = LocalView.current
    DisposableEffect(view) {
        val controller = WindowCompat.getInsetsController((view.context as Activity).window, view)
        val wasLight = controller.isAppearanceLightStatusBars
        controller.isAppearanceLightStatusBars = false
        onDispose { controller.isAppearanceLightStatusBars = wasLight }
    }

    if (uiState.isLoading || uiState.screenshots.isEmpty()) {
        Box(modifier = modifier.fillMaxSize().background(Color.Black))
        return
    }

    val pagerState = rememberPagerState(initialPage = uiState.initialIndex) { uiState.screenshots.size }

    LaunchedEffect(pagerState.currentPage) {
        viewModel.onPageChanged(pagerState.currentPage)
    }

    val current = uiState.screenshots[pagerState.currentPage.coerceIn(uiState.screenshots.indices)]
    val detail by viewModel.detailFor(current.id).collectAsState(initial = null)

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            ZoomableScreenshot(contentUri = uiState.screenshots[page].contentUri)
        }

        TopAppBar(
            title = {},
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = Color.White)
                }
            },
            actions = {
                val favoriteLabel = stringResource(
                    if (current.isFavorite) R.string.action_unfavorite else R.string.action_favorite,
                )
                IconButton(onClick = { viewModel.setFavorite(current.id, !current.isFavorite) }) {
                    Icon(
                        if (current.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = favoriteLabel,
                        tint = Color.White,
                    )
                }
                IconButton(onClick = { scope.launch { shareScreenshotUris(context, listOf(current.contentUri)) } }) {
                    Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.action_share), tint = Color.White)
                }
                IconButton(onClick = { viewModel.toggleDetails() }) {
                    Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.details_title), tint = Color.White)
                }
                IconButton(onClick = {
                    deleteGuard(1) {
                        scope.launch {
                            when (val outcome = viewModel.requestDelete(current.id)) {
                                is DeleteRequestOutcome.NeedsConfirmation -> {
                                    pendingDeleteIds = outcome.ids
                                    deleteLauncher.launch(IntentSenderRequest.Builder(outcome.pendingIntent).build())
                                }
                                is DeleteRequestOutcome.Deleted -> onBack()
                            }
                        }
                    }
                }) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete), tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            // Scrim keeps the white icons readable over light screenshots.
            modifier = Modifier
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)))
                .systemBarsPadding(),
        )
    }

    if (uiState.showDetails) {
        val allCollections by viewModel.collections.collectAsState()
        ScreenshotDetailsSheet(
            detail = detail,
            allCollections = allCollections,
            onDismiss = { viewModel.toggleDetails() },
            onNotesChanged = { notes -> viewModel.updateNotes(current.id, notes) },
            onAddTag = { tag -> viewModel.addTag(current.id, tag) },
            onRemoveTag = { tagId -> viewModel.removeTag(current.id, tagId) },
            onAddToCollection = { collectionId -> viewModel.addToCollection(current.id, collectionId) },
            onRemoveFromCollection = { collectionId -> viewModel.removeFromCollection(current.id, collectionId) },
        )
    }
}

@Composable
private fun ZoomableScreenshot(contentUri: String, modifier: Modifier = Modifier) {
    var scale by remember(contentUri) { mutableFloatStateOf(1f) }
    var offsetX by remember(contentUri) { mutableFloatStateOf(0f) }
    var offsetY by remember(contentUri) { mutableFloatStateOf(0f) }

    AsyncImage(
        model = contentUri,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .fillMaxSize()
            .pointerInput(contentUri) {
                detectTapGestures(
                    onDoubleTap = {
                        scale = if (scale > 1f) 1f else 2.5f
                        offsetX = 0f
                        offsetY = 0f
                    },
                )
            }
            .pointerInput(contentUri) {
                // detectTransformGestures eats one-finger drags, which blocks the pager swipe.
                // Only consume while pinching or zoomed in.
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        if (event.changes.size > 1 || scale > 1f) {
                            scale = (scale * event.calculateZoom()).coerceIn(1f, 5f)
                            val pan = event.calculatePan()
                            offsetX = if (scale <= 1f) 0f else offsetX + pan.x
                            offsetY = if (scale <= 1f) 0f else offsetY + pan.y
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offsetX,
                translationY = offsetY,
            ),
    )
}

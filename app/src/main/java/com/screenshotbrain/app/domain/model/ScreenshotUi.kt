package com.trove.app.domain.model

import com.trove.app.data.local.db.entity.ScreenshotEntity

/** Lightweight projection used by grids/lists so Compose never depends on the Room entity directly. */
data class ScreenshotUi(
    val id: Long,
    val contentUri: String,
    val displayName: String,
    val dateAddedSec: Long,
    val width: Int,
    val height: Int,
    val isFavorite: Boolean,
)

fun ScreenshotEntity.toUi() = ScreenshotUi(
    id = id,
    contentUri = contentUri,
    displayName = displayName,
    dateAddedSec = dateAddedSec,
    width = width,
    height = height,
    isFavorite = isFavorite,
)

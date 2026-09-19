package com.trove.app.domain.model

/** Timeline row: either a sticky-feeling section label or a screenshot. */
sealed interface LibraryListItem {
    data class Header(val label: String) : LibraryListItem
    data class Item(val screenshot: ScreenshotUi) : LibraryListItem
}

package com.trove.app.ui.navigation

import com.trove.app.ui.settings.SettingsSection
import java.net.URLDecoder
import java.net.URLEncoder

/** Identifies which paged list a [com.trove.app.ui.viewer.ScreenshotViewerScreen] pager should show. */
sealed class ScreenshotSource(val encoded: String) {
    data object All : ScreenshotSource("all")
    data class Category(val key: String) : ScreenshotSource("category:$key")
    data class Collection(val id: Long) : ScreenshotSource("collection:$id")
    data class SearchResults(val query: String) : ScreenshotSource("search:${URLEncoder.encode(query, "UTF-8")}")

    companion object {
        fun parse(encoded: String): ScreenshotSource = when {
            encoded == "all" -> All
            encoded.startsWith("category:") -> Category(encoded.removePrefix("category:"))
            encoded.startsWith("collection:") -> Collection(encoded.removePrefix("collection:").toLong())
            encoded.startsWith("search:") -> SearchResults(URLDecoder.decode(encoded.removePrefix("search:"), "UTF-8"))
            else -> All
        }
    }
}

object Destinations {
    const val HOME = "home"
    const val LIBRARY = "library"
    const val SEARCH = "search"
    const val COLLECTIONS = "collections"
    const val SETTINGS = "settings"

    const val LIBRARY_FULL = "library_full/{category}"
    fun libraryFull(categoryKey: String? = null) = "library_full/${categoryKey ?: "_"}"

    const val COLLECTION_DETAIL = "collection/{collectionId}/{name}"
    fun collectionDetail(collectionId: Long, name: String) =
        "collection/$collectionId/${URLEncoder.encode(name, "UTF-8")}"

    const val VIEWER = "viewer/{source}/{screenshotId}"
    fun viewer(source: ScreenshotSource, screenshotId: Long) = "viewer/${source.encoded}/$screenshotId"

    const val SETTINGS_SECTION = "settings/section/{section}"
    fun settingsSection(section: SettingsSection) = "settings/section/${section.name}"

    const val PRIVACY = "settings/privacy"
    const val DUPLICATES = "settings/duplicates"
}

package com.trove.app.domain.model

data class ScreenshotDetail(
    val screenshot: ScreenshotUi,
    val sizeBytes: Long,
    val mimeType: String,
    val sourceAppLabel: String?,
    val ocrText: String?,
    val categories: List<CategoryRefUi>,
    val tags: List<TagUi>,
    val notes: String?,
    val collections: List<CollectionUi>,
)

data class CategoryRefUi(val key: String, val displayName: String)

data class TagUi(val id: Long, val name: String)

data class CollectionUi(
    val id: Long,
    val name: String,
    val count: Int,
    val coverUri: String?,
)

data class IndexingProgress(
    val processed: Int,
    val total: Int,
    val isRunning: Boolean,
) {
    val isComplete: Boolean get() = !isRunning || (total > 0 && processed >= total)
}

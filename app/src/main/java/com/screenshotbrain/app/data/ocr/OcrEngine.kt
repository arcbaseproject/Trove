package com.trove.app.data.ocr

import android.net.Uri

/** Version bump forces re-OCR of already-processed screenshots (see [OcrResultEntity.engineVersion]). */
const val OCR_ENGINE_VERSION = 1

data class OcrOutcome(
    val text: String,
    val success: Boolean,
)

/**
 * Text recognition abstraction so the ML Kit implementation can be swapped (e.g. for a
 * higher-accuracy on-device model) without touching [com.trove.app.data.indexing.IndexingWorker].
 */
interface OcrEngine {
    suspend fun recognize(imageUri: Uri): OcrOutcome
}

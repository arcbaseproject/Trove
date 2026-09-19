package com.trove.app.data.classification

import com.trove.app.domain.model.CategoryKey

data class ClassificationInput(
    val ocrText: String,
    val displayName: String,
    val sourceAppPackage: String?,
    val sourceAppLabel: String?,
)

/**
 * Assigns zero or more [CategoryKey]s to a screenshot. [RuleBasedClassifier] is a deterministic
 * keyword/heuristic implementation; a future on-device model (e.g. Gemini Nano) implements this
 * same interface and is swapped in via Hilt without touching [com.trove.app.data.indexing.IndexingWorker]
 * or the database schema.
 */
interface Classifier {
    fun classify(input: ClassificationInput): Set<CategoryKey>
}

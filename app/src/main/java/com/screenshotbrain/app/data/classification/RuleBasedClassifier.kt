package com.trove.app.data.classification

import com.trove.app.domain.model.CategoryKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deterministic keyword + source-app heuristic classifier. Every rule is intentionally simple
 * and explainable; accuracy is expected to improve later by swapping the [Classifier] binding,
 * not by growing this file into a rules engine.
 */
@Singleton
class RuleBasedClassifier @Inject constructor() : Classifier {

    override fun classify(input: ClassificationInput): Set<CategoryKey> {
        val text = input.ocrText.lowercase()
        val name = input.displayName.lowercase()
        val pkg = input.sourceAppPackage?.lowercase().orEmpty()
        val results = mutableSetOf<CategoryKey>()

        if (pkg in MESSAGING_PACKAGES || TIMESTAMP.findAll(text).count() >= 3) {
            results += CategoryKey.CONVERSATIONS
        }
        if (pkg in SOCIAL_PACKAGES) {
            results += CategoryKey.SOCIAL_MEDIA
        }
        if (CODE_PATTERNS.count { it.containsMatchIn(text) } >= 2) {
            results += CategoryKey.CODE
        }
        if (RECEIPT_KEYWORDS.containsMatchIn(text)) {
            results += CategoryKey.RECEIPTS
        }
        if (PRODUCT_KEYWORDS.containsMatchIn(text)) {
            results += CategoryKey.PRODUCTS
        }
        if (pkg in MAPS_PACKAGES || MAPS_KEYWORDS.containsMatchIn(text)) {
            results += CategoryKey.MAPS
        }
        if (pkg in GAME_PACKAGES) {
            results += CategoryKey.GAMES
        }
        if (SCHOOL_KEYWORDS.containsMatchIn(text)) {
            results += CategoryKey.SCHOOL
        }
        if (DOCUMENT_KEYWORDS.containsMatchIn(text)) {
            results += CategoryKey.DOCUMENTS
        }
        if (MEME_KEYWORDS.containsMatchIn(text) || name.contains("meme")) {
            results += CategoryKey.MEMES
        }
        if (UI_DESIGN_KEYWORDS.containsMatchIn(text)) {
            results += CategoryKey.UI_DESIGN
        }

        if (results.isEmpty()) results += CategoryKey.OTHER
        return results
    }

    companion object {
        private val MESSAGING_PACKAGES = setOf(
            "com.whatsapp", "com.discord", "org.telegram.messenger", "com.google.android.apps.messaging",
            "com.facebook.orca", "com.microsoft.teams", "com.slack",
        )
        private val SOCIAL_PACKAGES = setOf(
            "com.instagram.android", "com.twitter.android", "com.zhiliaoapp.musically",
            "com.facebook.katana", "com.reddit.frontpage", "com.linkedin.android", "com.snapchat.android",
        )
        private val MAPS_PACKAGES = setOf("com.google.android.apps.maps", "com.waze")
        private val GAME_PACKAGES = setOf(
            "com.mojang.minecraftpe", "com.roblox.client", "com.supercell.clashofclans",
            "com.king.candycrushsaga", "com.epicgames.fortnite",
        )
        private fun words(vararg w: String) =
            Regex("(?<!\\w)(?:${w.joinToString("|") { Regex.escape(it) }})(?!\\w)")

        // One clock is the status bar; a chat has several message times.
        private val TIMESTAMP = Regex("""(?<!\d)\d{1,2}:\d{2}\s?(am|pm)?(?!\d)""")
        private val CODE_PATTERNS = listOf(
            Regex("""\bfun\s+\w+\("""), Regex("""\bdef\s+\w+\("""), Regex("""\bclass\s+\w+\s*[({:]"""),
            Regex("""[{};]\s*$""", RegexOption.MULTILINE), Regex("""\bimport\s+[\w.]+"""),
            Regex("""</\w+>"""), Regex("""\$\s?\w+\s?="""), Regex("""\b(const|val|var|let)\s+\w+\s*="""),
        )
        private val RECEIPT_KEYWORDS = words("subtotal", "receipt", "order confirmed", "order #", "amount paid", "total due")
        private val PRODUCT_KEYWORDS = Regex("""\$\s?\d+(\.\d{2})?|(?<!\w)(add to cart|buy now|in stock|free shipping)(?!\w)""")
        private val MAPS_KEYWORDS = words("directions", "eta", "miles away", "km away")
        private val SCHOOL_KEYWORDS = words("assignment", "due date", "homework", "syllabus", "canvas", "grade", "grades", "exam", "exams")
        private val DOCUMENT_KEYWORDS = words("invoice", "signature", "pdf", "agreement", "form")
        private val MEME_KEYWORDS = words("lol", "lmao", "meme", "nobody:")
        private val UI_DESIGN_KEYWORDS = words("figma", "dribbble", "design system", "wireframe", "prototype")
    }
}

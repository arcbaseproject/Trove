package com.trove.app.domain.model

/**
 * Fixed category taxonomy. A screenshot may carry several. [RuleBasedClassifier][com.trove.app.data.classification.RuleBasedClassifier]
 * assigns these today via keyword rules; the interface it implements
 * ([com.trove.app.data.classification.Classifier]) is the seam a future on-device
 * model plugs into without changing this list or the schema.
 */
enum class CategoryKey(val displayName: String) {
    CODE("Code"),
    CONVERSATIONS("Conversations"),
    PRODUCTS("Products"),
    RECEIPTS("Receipts"),
    MEMES("Memes"),
    SOCIAL_MEDIA("Social Media"),
    UI_DESIGN("UI / Design Inspiration"),
    SCHOOL("School"),
    DOCUMENTS("Documents"),
    MAPS("Maps"),
    GAMES("Games"),
    OTHER("Other"),
    ;

    companion object {
        fun fromKey(key: String): CategoryKey? = entries.find { it.name == key }
    }
}

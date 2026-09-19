package com.trove.app.data.repository

/**
 * Turns free-typed user input into a safe SQLite FTS4 MATCH expression: each token becomes a
 * prefix match (`term*`) ANDed together, so "5090" matches "RTX 5090 32GB" and partial words
 * still return results while the user is still typing.
 */
object FtsQuerySanitizer {
    private val TOKEN_REGEX = Regex("[\\p{L}\\p{Nd}]+")

    fun toMatchQuery(rawQuery: String): String? {
        val tokens = TOKEN_REGEX.findAll(rawQuery).map { it.value }.toList()
        if (tokens.isEmpty()) return null
        return tokens.joinToString(" ") { "\"$it\"*" }
    }
}

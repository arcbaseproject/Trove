package com.trove.app.util

import android.content.Intent
import android.net.Uri
import android.util.Patterns

data class OcrAction(val label: String, val intent: Intent)

fun ocrActions(text: String): List<OcrAction> {
    fun find(p: java.util.regex.Pattern, s: String) =
        p.matcher(s).let { m -> generateSequence { if (m.find()) m.group() else null }.toList() }

    val emails = find(Patterns.EMAIL_ADDRESS, text).distinct()
    val noEmails = emails.fold(text) { acc, e -> acc.replace(e, " ") }
    val urls = find(Patterns.WEB_URL, noEmails).distinct()
    val phones = find(Patterns.PHONE, noEmails).map { it.trim() }.filter { p -> p.count(Char::isDigit) in 7..15 }.distinct()

    return buildList {
        urls.forEach { u ->
            add(OcrAction(u, Intent(Intent.ACTION_VIEW, Uri.parse(if ("://" in u) u else "https://$u"))))
        }
        emails.forEach { add(OcrAction(it, Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$it")))) }
        phones.forEach { add(OcrAction(it, Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(it)}")))) }
    }.take(8)
}

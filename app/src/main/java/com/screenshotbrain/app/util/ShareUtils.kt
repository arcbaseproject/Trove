package com.trove.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri

fun shareScreenshotUris(context: Context, contentUris: List<String>) {
    if (contentUris.isEmpty()) return
    val uris = contentUris.map { Uri.parse(it) }
    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, uris.first())
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        }
    }
    intent.type = "image/*"
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(intent, null))
}

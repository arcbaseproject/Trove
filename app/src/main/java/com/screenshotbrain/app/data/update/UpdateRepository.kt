package com.trove.app.data.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.getSystemService
import com.trove.app.BuildConfig
import com.trove.app.util.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val LATEST_RELEASE_URL = "https://api.github.com/repos/arcbaseproject/Trove/releases/latest"
private const val APK_MIME = "application/vnd.android.package-archive"

/**
 * Installers that mean "sideloaded by hand": no store will update Trove, so the app offers updates itself.
 * Anything else (Obtainium, Orion Store, F-Droid, Play) owns updates and the launch prompt stays quiet.
 * shortcut: Shizuku installs report com.android.shell too, so a store using Shizuku still gets the prompt.
 */
private val SIDELOAD_INSTALLERS = setOf(
    "com.android.packageinstaller",
    "com.google.android.packageinstaller",
    "com.android.shell",
)

data class Release(val version: String, val apkUrl: String, val notes: String)

/**
 * Checks GitHub releases for a newer APK and hands the download to the platform DownloadManager.
 * No HTTP client dependency: one GET of a small JSON document is what HttpURLConnection is for.
 */
@Singleton
class UpdateRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) {

    /** False when a store installed Trove; that store handles updates, so don't prompt on launch. */
    val updatesItself: Boolean by lazy {
        val installer = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(context.packageName)
            }
        }.getOrNull()
        installer == null || installer in SIDELOAD_INSTALLERS
    }

    /** Newer release, or null when up to date, offline, or the release has no APK asset. */
    suspend fun newerRelease(): Release? = withContext(io) {
        val release = runCatching { fetchLatest() }.getOrNull() ?: return@withContext null
        release.takeIf { isNewer(it.version, BuildConfig.VERSION_NAME) }
    }

    private fun fetchLatest(): Release? {
        val connection = (URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection).apply {
            setRequestProperty("Accept", "application/vnd.github+json")
            connectTimeout = 10_000
            readTimeout = 10_000
        }
        val body = try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
        val json = JSONObject(body)
        val assets = json.getJSONArray("assets")
        val apkUrl = (0 until assets.length())
            .map { assets.getJSONObject(it) }
            .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) }
            ?.optString("browser_download_url")
            ?: return null
        return Release(
            version = json.optString("tag_name").removePrefix("v"),
            apkUrl = apkUrl,
            notes = json.optString("body").trim(),
        )
    }

    /** Downloads the APK, then opens the system installer. Suspends until the download settles. */
    suspend fun downloadAndInstall(release: Release, onProgress: (Float) -> Unit = {}): Boolean = withContext(io) {
        val manager = context.getSystemService<DownloadManager>() ?: return@withContext false
        val request = DownloadManager.Request(Uri.parse(release.apkUrl))
            .setTitle("Trove ${release.version}")
            .setMimeType(APK_MIME)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "trove-${release.version}.apk")
        val id = manager.enqueue(request)

        // ponytail: polls the download instead of registering a receiver; one dialog is waiting on it anyway.
        var status = statusOf(manager, id, onProgress)
        while (status != DownloadManager.STATUS_SUCCESSFUL && status != DownloadManager.STATUS_FAILED && status != null) {
            delay(500)
            status = statusOf(manager, id, onProgress)
        }
        if (status != DownloadManager.STATUS_SUCCESSFUL) return@withContext false
        manager.getUriForDownloadedFile(id)?.let { install(it) }
        true
    }

    private fun statusOf(manager: DownloadManager, id: Long, onProgress: (Float) -> Unit): Int? =
        manager.query(DownloadManager.Query().setFilterById(id)).use { cursor ->
            if (!cursor.moveToFirst()) return null
            val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            val done = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            if (total > 0) onProgress(done.toFloat() / total)
            cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
        }

    private fun install(apk: Uri) {
        // Sideloaded installs need the user's "install unknown apps" grant first.
        if (!context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return // The APK is downloaded; tapping Update again installs it once the grant is given.
        }
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(apk, APK_MIME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION),
        )
    }
}

/** Numeric, dot-separated compare ("0.10.0" beats "0.9.9"); non-numeric suffixes are ignored. */
internal fun isNewer(latest: String, current: String): Boolean {
    fun parts(v: String) = v.trim().removePrefix("v").split(".", "-")
        .mapNotNull { it.takeWhile(Char::isDigit).toIntOrNull() }
    val a = parts(latest)
    val b = parts(current)
    if (a.isEmpty()) return false
    for (i in 0 until maxOf(a.size, b.size)) {
        val x = a.getOrElse(i) { 0 }
        val y = b.getOrElse(i) { 0 }
        if (x != y) return x > y
    }
    return false
}

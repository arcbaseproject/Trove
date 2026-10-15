package com.trove.app.data.mediastore

import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.trove.app.data.local.db.entity.ScreenshotEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads screenshot metadata straight from [MediaStore.Images.Media]. No file is opened here —
 * only the columns needed to populate [ScreenshotEntity]. Screenshots are recognised by the
 * "Screenshots" bucket/relative-path used by AOSP and every major OEM, plus the common
 * `Screenshot_*` filename pattern as a fallback for devices that store them elsewhere.
 */
@Singleton
class MediaStoreScreenshotSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun queryAllScreenshots(): List<DiscoveredScreenshot> {
        val results = mutableListOf<DiscoveredScreenshot>()
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        // RELATIVE_PATH only exists on API 29+; older versions expose the absolute file path instead.
        @Suppress("DEPRECATION")
        val pathColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.RELATIVE_PATH
        } else {
            MediaStore.Images.Media.DATA
        }
        val projection = buildList {
            add(MediaStore.Images.Media._ID)
            add(MediaStore.Images.Media.DISPLAY_NAME)
            add(MediaStore.Images.Media.DATE_ADDED)
            add(MediaStore.Images.Media.DATE_MODIFIED)
            add(MediaStore.Images.Media.WIDTH)
            add(MediaStore.Images.Media.HEIGHT)
            add(MediaStore.Images.Media.SIZE)
            add(MediaStore.Images.Media.MIME_TYPE)
            add(pathColumn)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Images.Media.OWNER_PACKAGE_NAME)
            }
        }.toTypedArray()

        val selection = "$pathColumn LIKE ? OR " +
            "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%Screenshot%", "Screenshot%")

        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Images.Media.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val addedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val modifiedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val pathCol = cursor.getColumnIndexOrThrow(pathColumn)
            val ownerCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.Images.Media.OWNER_PACKAGE_NAME)
            } else {
                -1
            }

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val ownerPackage = if (ownerCol >= 0) cursor.getString(ownerCol) else null
                results += DiscoveredScreenshot(
                    mediaStoreId = id,
                    contentUri = ContentUris.withAppendedId(collection, id).toString(),
                    displayName = cursor.getString(nameCol) ?: "screenshot_$id",
                    dateAddedSec = cursor.getLong(addedCol),
                    dateModifiedSec = cursor.getLong(modifiedCol),
                    width = cursor.getInt(widthCol),
                    height = cursor.getInt(heightCol),
                    sizeBytes = cursor.getLong(sizeCol),
                    mimeType = cursor.getString(mimeCol) ?: "image/*",
                    bucketPath = cursor.getString(pathCol),
                    sourceAppPackage = ownerPackage,
                    sourceAppLabel = ownerPackage?.let { resolveAppLabel(it) },
                )
            }
        }
        return results
    }

    private fun resolveAppLabel(packageName: String): String? = try {
        val pm = context.packageManager
        val info = pm.getApplicationInfo(packageName, 0)
        pm.getApplicationLabel(info).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    fun hasReadPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}

data class DiscoveredScreenshot(
    val mediaStoreId: Long,
    val contentUri: String,
    val displayName: String,
    val dateAddedSec: Long,
    val dateModifiedSec: Long,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val mimeType: String,
    val bucketPath: String?,
    val sourceAppPackage: String?,
    val sourceAppLabel: String?,
)

fun DiscoveredScreenshot.toEntity() = ScreenshotEntity(
    mediaStoreId = mediaStoreId,
    contentUri = contentUri,
    displayName = displayName,
    dateAddedSec = dateAddedSec,
    dateModifiedSec = dateModifiedSec,
    width = width,
    height = height,
    sizeBytes = sizeBytes,
    mimeType = mimeType,
    sourceAppPackage = sourceAppPackage,
    sourceAppLabel = sourceAppLabel,
    bucketPath = bucketPath,
)

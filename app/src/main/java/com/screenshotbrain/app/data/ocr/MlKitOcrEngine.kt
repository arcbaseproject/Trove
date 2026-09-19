package com.trove.app.data.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

/** Runs entirely on-device via the bundled Latin-script model; no network call is ever made. */
@Singleton
class MlKitOcrEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) : OcrEngine {

    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    override suspend fun recognize(imageUri: Uri): OcrOutcome = try {
        val image = InputImage.fromFilePath(context, imageUri)
        val result = recognizer.process(image).await()
        OcrOutcome(text = result.text, success = true)
    } catch (e: Exception) {
        OcrOutcome(text = "", success = false)
    }
}

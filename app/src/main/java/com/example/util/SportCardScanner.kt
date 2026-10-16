package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.Rect
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object SportCardScanner {

    private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result ->
            if (cont.isActive) cont.resume(result)
        }
        addOnFailureListener { exception ->
            if (cont.isActive) cont.resumeWithException(exception)
        }
        addOnCanceledListener {
            if (cont.isActive) cont.cancel()
        }
    }

    suspend fun processCardBitmap(context: Context, bitmap: Bitmap): Result<ExtractedMemberInfo> =
        withContext(Dispatchers.IO) {
            try {
                val inputImage = InputImage.fromBitmap(bitmap, 0)

                // 1. OCR Text Recognition via ML Kit
                var ocrFullText = ""
                val ocrLines = mutableListOf<String>()
                try {
                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    val visionText = recognizer.process(inputImage).awaitTask()
                    ocrFullText = visionText.text
                    for (block in visionText.textBlocks) {
                        for (line in block.lines) {
                            if (line.text.isNotBlank()) {
                                ocrLines.add(line.text)
                            }
                        }
                    }
                } catch (_: Exception) {
                    // OCR failed or unavailable
                }

                // 2. Face / Portrait Detection (ML Kit + Native Fallback)
                var detectedFaceBounds: Rect? = null
                try {
                    val faceOptions = FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                        .setMinFaceSize(0.08f)
                        .build()
                    val faceDetector = FaceDetection.getClient(faceOptions)
                    val faces = faceDetector.process(inputImage).awaitTask()
                    if (faces.isNotEmpty()) {
                        val largestFace = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                        if (largestFace != null) {
                            detectedFaceBounds = largestFace.boundingBox
                        }
                    }
                } catch (_: Exception) {
                    // Face detection via ML Kit failed
                }

                // Native android.media.FaceDetector fallback if ML Kit didn't find a face
                if (detectedFaceBounds == null) {
                    detectedFaceBounds = detectFaceWithNativeApi(bitmap)
                }

                // 3. Crop Portrait strictly if a face was detected
                var portraitPhotoPath = ""
                var portraitDetected = false
                if (detectedFaceBounds != null) {
                    val croppedBitmap = cropPortrait(bitmap, detectedFaceBounds)
                    if (croppedBitmap != null) {
                        portraitPhotoPath = savePortraitToFile(context, croppedBitmap)
                        if (portraitPhotoPath.isNotBlank()) {
                            portraitDetected = true
                        }
                    }
                }

                // 4. Barcode / QR Code Scanning (ML Kit + ZXing)
                var scannedQrText: String? = null
                try {
                    val barcodeScanner = BarcodeScanning.getClient()
                    val barcodes = barcodeScanner.process(inputImage).awaitTask()
                    scannedQrText = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
                } catch (_: Exception) {
                    // Barcode scanning via ML Kit failed
                }

                if (scannedQrText.isNullOrBlank()) {
                    scannedQrText = decodeQrWithZxing(bitmap)
                }

                // 5. Data Extraction and Mapping
                val qrInfo = if (!scannedQrText.isNullOrBlank()) {
                    CardDataParser.parseCardData(scannedQrText)
                } else null

                val ocrInfo = if (ocrFullText.isNotBlank() || ocrLines.isNotEmpty()) {
                    CardDataParser.parseOcrText(ocrFullText, ocrLines)
                } else null

                // Merge recognized fields
                val finalExtracted = CardDataParser.mergeInfo(
                    qrInfo = qrInfo,
                    ocrInfo = ocrInfo,
                    croppedPortraitPath = portraitPhotoPath,
                    portraitDetected = portraitDetected
                )

                Result.success(finalExtracted)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Fallback face detector using Android's built-in android.media.FaceDetector.
     */
    fun detectFaceWithNativeApi(bitmap: Bitmap): Rect? {
        return try {
            val rgb565 = if (bitmap.config == Bitmap.Config.RGB_565) {
                bitmap
            } else {
                bitmap.copy(Bitmap.Config.RGB_565, false)
            } ?: return null

            // Width must be even for android.media.FaceDetector
            val safeWidth = if (rgb565.width % 2 == 0) rgb565.width else rgb565.width - 1
            val workingBitmap = if (safeWidth == rgb565.width) {
                rgb565
            } else {
                Bitmap.createBitmap(rgb565, 0, 0, safeWidth, rgb565.height)
            }

            val detector = android.media.FaceDetector(workingBitmap.width, workingBitmap.height, 1)
            val faces = arrayOfNulls<android.media.FaceDetector.Face>(1)
            val count = detector.findFaces(workingBitmap, faces)
            if (count > 0 && faces[0] != null) {
                val face = faces[0]!!
                val midPoint = PointF()
                face.getMidPoint(midPoint)
                val eyeDistance = face.eyesDistance()
                if (eyeDistance > 5f) {
                    val halfWidth = (eyeDistance * 1.5f).toInt()
                    val topMargin = (eyeDistance * 1.5f).toInt()
                    val bottomMargin = (eyeDistance * 2.2f).toInt()

                    val left = (midPoint.x - halfWidth).toInt().coerceIn(0, bitmap.width - 1)
                    val top = (midPoint.y - topMargin).toInt().coerceIn(0, bitmap.height - 1)
                    val right = (midPoint.x + halfWidth).toInt().coerceIn(left + 1, bitmap.width)
                    val bottom = (midPoint.y + bottomMargin).toInt().coerceIn(top + 1, bitmap.height)
                    Rect(left, top, right, bottom)
                } else null
            } else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Crops the athlete's portrait keeping reasonable margins around head and shoulders.
     * Never returns the whole document.
     */
    fun cropPortrait(bitmap: Bitmap, faceBox: Rect): Bitmap? {
        val faceWidth = faceBox.width()
        val faceHeight = faceBox.height()
        if (faceWidth <= 10 || faceHeight <= 10) return null

        val centerX = faceBox.centerX()
        val centerY = faceBox.centerY()

        // Portrait dimensions: ~1.75x width of face, ~2.2x height of face
        val cropWidth = (faceWidth * 1.75f).toInt().coerceAtLeast(faceWidth)
        val cropHeight = (faceHeight * 2.25f).toInt().coerceAtLeast(faceHeight)

        // Head room above (face center is at ~42% of portrait height)
        val left = (centerX - cropWidth / 2).coerceIn(0, bitmap.width - 1)
        val top = (centerY - (cropHeight * 0.42f).toInt()).coerceIn(0, bitmap.height - 1)
        val right = (left + cropWidth).coerceAtMost(bitmap.width)
        val bottom = (top + cropHeight).coerceAtMost(bitmap.height)

        val actualW = right - left
        val actualH = bottom - top
        if (actualW < 20 || actualH < 20) return null

        // Ensure we don't return 98%+ of the document image as a "portrait"
        val isEntireImage = (actualW >= bitmap.width * 0.95f) && (actualH >= bitmap.height * 0.95f)
        if (isEntireImage) return null

        return try {
            Bitmap.createBitmap(bitmap, left, top, actualW, actualH)
        } catch (_: Exception) {
            null
        }
    }

    private fun savePortraitToFile(context: Context, portraitBitmap: Bitmap): String {
        return try {
            val dir = File(context.cacheDir, "judoka_portraits").apply { mkdirs() }
            val file = File(dir, "portrait_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                portraitBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            file.absolutePath
        } catch (_: Exception) {
            ""
        }
    }

    private fun decodeQrWithZxing(bitmap: Bitmap): String? {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val reader = MultiFormatReader()
            val result = reader.decode(binaryBitmap)
            result.text
        } catch (_: Exception) {
            null
        }
    }
}

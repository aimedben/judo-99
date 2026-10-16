package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

object QrCodeGenerator {
    fun generateQrBitmap(content: String, size: Int = 512): Bitmap {
        if (content.isBlank()) {
            val emptyBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            emptyBitmap.eraseColor(Color.WHITE)
            return emptyBitmap
        }
        return try {
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (_: Exception) {
            val fallback = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            fallback.eraseColor(Color.WHITE)
            fallback
        }
    }

    fun generateQrImageBitmap(content: String, size: Int = 512): ImageBitmap {
        return generateQrBitmap(content, size).asImageBitmap()
    }
}

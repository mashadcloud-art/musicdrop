package com.musicdrop.tv.util

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QRCodeGenerator {
    fun generateQRCode(
        content: String,
        sizePx: Int = 512,
        fgColor: Int = 0xFFD2F801.toInt(), // Electric Lime
        bgColor: Int = 0xFF140E2A.toInt()  // Deep Obsidian Purple
    ): ImageBitmap? {
        return try {
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 1
            )
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)

            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) fgColor else bgColor
                }
            }

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap.asImageBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun generateQuickShareQRCode(
        content: String,
        sizePx: Int = 512
    ): ImageBitmap? {
        return try {
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
                EncodeHintType.MARGIN to 1
            )
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)

            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) 0xFF1F1A17.toInt() else 0xFFFFFFFF.toInt()
                }
            }

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)

            // Draw center circular badge with Quick Share dual-arrow symbol
            val canvas = android.graphics.Canvas(bitmap)
            val centerX = width / 2f
            val centerY = height / 2f
            val radius = width * 0.11f // ~22% diameter (well within Level H 30% tolerance)

            // Outer white padding circle
            val bgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFFFFFFF.toInt()
                style = android.graphics.Paint.Style.FILL
            }
            canvas.drawCircle(centerX, centerY, radius + 4f, bgPaint)

            // Dark inner circle badge
            val badgePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF1F1A17.toInt()
                style = android.graphics.Paint.Style.FILL
            }
            canvas.drawCircle(centerX, centerY, radius, badgePaint)

            // Draw dual arrows symbol
            val strokePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFFFFFFF.toInt()
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = width * 0.015f
                strokeCap = android.graphics.Paint.Cap.ROUND
                strokeJoin = android.graphics.Paint.Join.ROUND
            }

            val arrowLen = radius * 0.55f
            val arrowHead = radius * 0.22f
            val yOffset = radius * 0.25f

            // Top arrow pointing right ->
            val topY = centerY - yOffset
            canvas.drawLine(centerX - arrowLen, topY, centerX + arrowLen, topY, strokePaint)
            canvas.drawLine(centerX + arrowLen - arrowHead, topY - arrowHead, centerX + arrowLen, topY, strokePaint)
            canvas.drawLine(centerX + arrowLen - arrowHead, topY + arrowHead, centerX + arrowLen, topY, strokePaint)

            // Bottom arrow pointing left <-
            val bottomY = centerY + yOffset
            canvas.drawLine(centerX + arrowLen, bottomY, centerX - arrowLen, bottomY, strokePaint)
            canvas.drawLine(centerX - arrowLen + arrowHead, bottomY - arrowHead, centerX - arrowLen, bottomY, strokePaint)
            canvas.drawLine(centerX - arrowLen + arrowHead, bottomY + arrowHead, centerX - arrowLen, bottomY, strokePaint)

            bitmap.asImageBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: standard clean QR code without overlay if canvas drawing fails
            generateQRCode(content, sizePx, 0xFF1F1A17.toInt(), 0xFFFFFFFF.toInt())
        }
    }
}

package com.example.healthai.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

object ImageUtils {

    /** 把 Uri（相册/拍照文件）读成压缩后的 base64 JPEG，直接喂给视觉 API。 */
    fun uriToCompressedBase64(
        context: Context,
        uri: Uri,
        maxDim: Int = 1024,
        quality: Int = 80
    ): String {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return ""
            val bitmap = BitmapFactory.decodeStream(input)
            input.close()
            if (bitmap == null) return ""
            val scaled = scaleDown(bitmap, maxDim)
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            if (scaled != bitmap) bitmap.recycle()
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            ""
        }
    }

    private fun scaleDown(src: Bitmap, maxDim: Int): Bitmap {
        val w = src.width
        val h = src.height
        val ratio = minOf(maxDim.toFloat() / w, maxDim.toFloat() / h)
        if (ratio >= 1f) return src
        return Bitmap.createScaledBitmap(src, (w * ratio).toInt(), (h * ratio).toInt(), true)
    }

    fun base64ToBitmap(base64: String): Bitmap? {
        return try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }
}

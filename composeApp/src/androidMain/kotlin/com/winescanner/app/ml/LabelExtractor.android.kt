package com.winescanner.app.ml

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.FloatBuffer

actual class LabelExtractor(private val context: Context) {

    private val ortEnvironment: OrtEnvironment by lazy { OrtEnvironment.getEnvironment() }


    private val session: OrtSession? by lazy { runCatching { loadSession() }.getOrNull() }

    private fun loadSession(): OrtSession {
        val modelBytes = context.assets.open(MODEL_ASSET_PATH).use { it.readBytes() }
        return ortEnvironment.createSession(modelBytes)
    }

    actual suspend fun extractLabelCrop(photoBytes: ByteArray): ByteArray = withContext(Dispatchers.Default) {
        val original = BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.size)
            ?: throw IllegalArgumentException("Не удалось декодировать снимок с камеры")

        val bounds = runCatching { detectLabelBounds(original) }.getOrElse { CENTER_CROP_FALLBACK }

        cropAndEncode(original, bounds)
    }


    private fun detectLabelBounds(bitmap: Bitmap): LabelBounds {
        val activeSession = session ?: error("ONNX-сессия недоступна")
        val inputName = activeSession.inputNames.first()

        val resized = Bitmap.createScaledBitmap(bitmap, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE, true)
        val inputBuffer = resized.toNormalizedFloatBuffer()
        val shape = longArrayOf(1, 3, MODEL_INPUT_SIZE.toLong(), MODEL_INPUT_SIZE.toLong())

        OnnxTensor.createTensor(ortEnvironment, inputBuffer, shape).use { tensor ->
            activeSession.run(mapOf(inputName to tensor)).use { results ->
                @Suppress("UNCHECKED_CAST")
                val output = results[0].value as Array<FloatArray>
                val (x1, y1, x2, y2, confidence) = output[0]
                if (confidence < MIN_CONFIDENCE) error("Низкая уверенность детекции: $confidence")
                return LabelBounds(
                    left = x1.coerceIn(0f, 1f),
                    top = y1.coerceIn(0f, 1f),
                    right = x2.coerceIn(0f, 1f),
                    bottom = y2.coerceIn(0f, 1f)
                )
            }
        }
    }

    private fun cropAndEncode(bitmap: Bitmap, bounds: LabelBounds): ByteArray {
        val left = (bounds.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val top = (bounds.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val right = (bounds.right * bitmap.width).toInt().coerceIn(left + 1, bitmap.width)
        val bottom = (bounds.bottom * bitmap.height).toInt().coerceIn(top + 1, bitmap.height)

        val cropped = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
        val output = ByteArrayOutputStream()
        cropped.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
        return output.toByteArray()
    }

    private companion object {
        const val MODEL_ASSET_PATH = "best.onnx"
        const val MODEL_INPUT_SIZE = 384
        const val MIN_CONFIDENCE = 0.4f
        const val JPEG_QUALITY = 90
        val CENTER_CROP_FALLBACK = LabelBounds(0.15f, 0.15f, 0.85f, 0.85f)
    }
}


private fun Bitmap.toNormalizedFloatBuffer(): FloatBuffer {
    val buffer = FloatBuffer.allocate(3 * width * height)
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)

    for (channel in 0 until 3) {
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val value = when (channel) {
                0 -> (pixel shr 16 and 0xFF)
                1 -> (pixel shr 8 and 0xFF)
                else -> (pixel and 0xFF)
            }
            buffer.put(channel * pixels.size + i, value / 255f)
        }
    }
    buffer.rewind()
    return buffer
}

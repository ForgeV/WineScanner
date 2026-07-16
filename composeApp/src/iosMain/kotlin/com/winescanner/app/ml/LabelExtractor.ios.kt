package com.winescanner.app.ml

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.memcpy
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGImageCreateWithImageInRect
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation


@OptIn(ExperimentalForeignApi::class)
actual class LabelExtractor {

    actual suspend fun extractLabelCrop(photoBytes: ByteArray): ByteArray = withContext(Dispatchers.Default) {
        val image = photoBytes.toUIImage()
            ?: throw IllegalArgumentException("Не удалось декодировать снимок с камеры")

        val bounds = runCatching { detectLabelBounds(image) }.getOrElse { CENTER_CROP_FALLBACK }

        cropAndEncode(image, bounds)
    }

    private fun detectLabelBounds(image: UIImage): LabelBounds {
        // TODO: подключить ONNX Runtime через CocoaPod onnxruntime-objc.

        error("ONNX-инференс на iOS ещё не подключён — см. TODO выше")
    }

    private fun cropAndEncode(image: UIImage, bounds: LabelBounds): ByteArray {
        val cgImage = image.CGImage ?: throw IllegalStateException("Нет CGImage у снимка")
        val width = CGImageGetWidth(cgImage).toDouble()
        val height = CGImageGetHeight(cgImage).toDouble()

        val cropRect = CGRectMake(
            x = bounds.left.toDouble() * width,
            y = bounds.top.toDouble() * height,
            width = (bounds.right - bounds.left).toDouble() * width,
            height = (bounds.bottom - bounds.top).toDouble() * height
        )
        val croppedRef = CGImageCreateWithImageInRect(cgImage, cropRect)
            ?: throw IllegalStateException("Не удалось выполнить кроп изображения")
        val croppedImage = UIImage.imageWithCGImage(croppedRef)
        val jpegData = UIImageJPEGRepresentation(croppedImage, 0.9)
            ?: throw IllegalStateException("Не удалось закодировать JPEG")
        return jpegData.toByteArray()
    }

    private companion object {
        val CENTER_CROP_FALLBACK = LabelBounds(0.15f, 0.15f, 0.85f, 0.85f)
    }
}

private fun ByteArray.toUIImage(): UIImage? = usePinned { pinned ->
    val data = NSData.create(bytes = pinned.addressOf(0), length = this.size.convert())
    UIImage(data = data)
}

private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val result = ByteArray(size)
    if (size > 0) {
        result.usePinned { pinned ->
            memcpy(pinned.addressOf(0), bytes, length.convert())
        }
    }
    return result
}

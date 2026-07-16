package com.winescanner.app.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.memcpy
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCapturePhoto
import platform.AVFoundation.AVCapturePhotoCaptureDelegateProtocol
import platform.AVFoundation.AVCapturePhotoOutput
import platform.AVFoundation.AVCapturePhotoSettings
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPresetPhoto
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.fileDataRepresentation
import platform.AVFoundation.requestAccessForMediaType
import platform.Foundation.NSError
import platform.UIKit.UIView
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalForeignApi::class)
actual class CameraController {
    internal val session = AVCaptureSession().apply {
        sessionPreset = AVCaptureSessionPresetPhoto
    }
    internal val photoOutput = AVCapturePhotoOutput()


    private var pendingDelegate: PhotoCaptureDelegate? = null
    private var isConfigured = false

    internal fun ensureConfigured() {
        if (isConfigured) return
        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) ?: return
        val input = AVCaptureDeviceInput.deviceInputWithDevice(device, null) ?: return
        if (session.canAddInput(input)) session.addInput(input)
        if (session.canAddOutput(photoOutput)) session.addOutput(photoOutput)
        session.startRunning()
        isConfigured = true
    }

    actual suspend fun capturePhoto(): ByteArray = suspendCancellableCoroutine { continuation ->
        ensureConfigured()
        val delegate = PhotoCaptureDelegate { bytes, errorMessage ->
            pendingDelegate = null
            if (bytes != null) {
                continuation.resume(bytes)
            } else {
                continuation.resumeWithException(RuntimeException(errorMessage ?: "Ошибка захвата кадра"))
            }
        }
        pendingDelegate = delegate
        photoOutput.capturePhotoWithSettings(AVCapturePhotoSettings(), delegate)
    }
}

@OptIn(ExperimentalForeignApi::class)
private class PhotoCaptureDelegate(
    private val onResult: (ByteArray?, String?) -> Unit
) : NSObject(), AVCapturePhotoCaptureDelegateProtocol {

    override fun captureOutput(
        output: AVCapturePhotoOutput,
        didFinishProcessingPhoto: AVCapturePhoto,
        error: NSError?
    ) {
        if (error != null) {
            onResult(null, error.localizedDescription)
            return
        }
        val data = didFinishProcessingPhoto.fileDataRepresentation()
        if (data == null) {
            onResult(null, "Нет данных изображения")
            return
        }
        val bytes = ByteArray(data.length.toInt())
        if (bytes.isNotEmpty()) {
            bytes.usePinned { pinned ->
                memcpy(pinned.addressOf(0), data.bytes, data.length.convert())
            }
        }
        onResult(bytes, null)
    }
}

@Composable
actual fun rememberCameraController(): CameraController = remember { CameraController() }

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun CameraPreview(
    controller: CameraController,
    modifier: Modifier
) {
    var isAuthorized by remember {
        mutableStateOf(
            AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo) == AVAuthorizationStatusAuthorized
        )
    }

    LaunchedEffect(Unit) {
        val status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
        if (status == AVAuthorizationStatusNotDetermined) {
            isAuthorized = suspendCancellableCoroutine { continuation ->
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                    continuation.resume(granted)
                }
            }
        }
    }

    if (!isAuthorized) {
        Box(modifier = modifier.background(Color.Black), contentAlignment = Alignment.Center) {
            Text("Нужен доступ к камере", color = Color.White)
        }
        return
    }

    UIKitView(
        modifier = modifier,
        factory = {
            controller.ensureConfigured()
            val previewLayer = AVCaptureVideoPreviewLayer(session = controller.session)
            previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill

            object : UIView() {
                override fun layoutSubviews() {
                    super.layoutSubviews()
                    previewLayer.frame = bounds
                }
            }.apply {
                layer.addSublayer(previewLayer)
            }
        }
    )
}

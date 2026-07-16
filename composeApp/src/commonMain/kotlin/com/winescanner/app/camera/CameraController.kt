package com.winescanner.app.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier


expect class CameraController {
    suspend fun capturePhoto(): ByteArray
}

@Composable
expect fun rememberCameraController(): CameraController

@Composable
expect fun CameraPreview(
    controller: CameraController,
    modifier: Modifier
)

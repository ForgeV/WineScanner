package com.winescanner.app.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.winescanner.app.camera.CameraPreview
import com.winescanner.app.camera.rememberCameraController
import com.winescanner.app.ui.theme.WineColors
import kotlinx.coroutines.launch

@Composable
fun CameraScreen(
    errorMessage: String?,
    onPhotoCaptured: (ByteArray) -> Unit
) {
    val controller = rememberCameraController()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isCapturing by remember { mutableStateOf(false) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        containerColor = Color.Black,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            CameraPreview(
                controller = controller,
                modifier = Modifier.fillMaxSize()
            )

            // Рамка-подсказка: куда навести этикетку.
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.72f)
                    .fillMaxHeight(0.48f)
                    .border(2.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 44.dp)
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Наведите камеру на этикетку вина",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
                Box(modifier = Modifier.padding(top = 20.dp)) {
                    ShutterButton(
                        isCapturing = isCapturing,
                        onClick = {
                            if (!isCapturing) {
                                isCapturing = true
                                scope.launch {
                                    try {
                                        val photo = controller.capturePhoto()
                                        onPhotoCaptured(photo)
                                    } catch (t: Throwable) {
                                        snackbarHostState.showSnackbar("Не удалось сделать снимок, попробуйте ещё раз")
                                    } finally {
                                        isCapturing = false
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}


@Composable
private fun ShutterButton(
    isCapturing: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(78.dp)
            .border(3.dp, WineColors.Gold, CircleShape)
            .padding(6.dp)
            .clip(CircleShape)
            .background(if (isCapturing) Color.Gray else WineColors.Cream)
            .clickable(enabled = !isCapturing, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isCapturing) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        }
    }
}

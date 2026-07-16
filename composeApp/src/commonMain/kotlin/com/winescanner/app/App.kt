package com.winescanner.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.winescanner.app.domain.ScanRepository
import com.winescanner.app.ml.LabelExtractor
import com.winescanner.app.model.ScanUiState
import com.winescanner.app.network.ApiConfig
import com.winescanner.app.network.NetworkModule
import com.winescanner.app.network.WineApiService
import com.winescanner.app.presentation.CameraScreen
import com.winescanner.app.presentation.LoadingScreen
import com.winescanner.app.presentation.ResultScreen
import com.winescanner.app.presentation.ScanViewModel
import com.winescanner.app.ui.theme.WineScannerTheme


@Composable
fun App(labelExtractor: LabelExtractor) {
    WineScannerTheme {
        val coroutineScope = rememberCoroutineScope()
        val viewModel = remember {
            val httpClient = NetworkModule.createHttpClient()
            val apiService = WineApiService(httpClient, ApiConfig.BASE_URL)
            val repository = ScanRepository(labelExtractor, apiService)
            ScanViewModel(repository, coroutineScope)
        }

        val uiState by viewModel.uiState.collectAsState()

        when (val state = uiState) {
            is ScanUiState.Camera -> CameraScreen(
                errorMessage = state.errorMessage,
                onPhotoCaptured = viewModel::onPhotoCaptured
            )

            is ScanUiState.Loading -> LoadingScreen()

            is ScanUiState.Result -> ResultScreen(
                wine = state.wine,
                onBackClick = viewModel::onBackToCamera
            )
        }
    }
}

package com.winescanner.app.presentation

import com.winescanner.app.domain.ScanRepository
import com.winescanner.app.model.ScanUiState
import com.winescanner.app.network.WineApiException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class ScanViewModel(
    private val repository: ScanRepository,
    private val coroutineScope: CoroutineScope
) {
    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Camera())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    fun onPhotoCaptured(photoBytes: ByteArray) {
        _uiState.value = ScanUiState.Loading
        coroutineScope.launch {
            runCatching { repository.identifyWine(photoBytes) }
                .onSuccess { wine -> _uiState.value = ScanUiState.Result(wine) }
                .onFailure { error -> _uiState.value = ScanUiState.Camera(errorMessage = error.toUserMessage()) }
        }
    }

    fun onBackToCamera() {
        _uiState.value = ScanUiState.Camera()
    }

    private fun Throwable.toUserMessage(): String = when (this) {
        is WineApiException -> "Сервер недоступен, попробуйте ещё раз"
        else -> "Не удалось распознать этикетку, попробуйте ещё раз"
    }
}

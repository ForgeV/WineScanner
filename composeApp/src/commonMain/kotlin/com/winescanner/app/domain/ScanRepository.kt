package com.winescanner.app.domain

import com.winescanner.app.ml.LabelExtractor
import com.winescanner.app.model.WineResult
import com.winescanner.app.network.WineApiService


class ScanRepository(
    private val labelExtractor: LabelExtractor,
    private val apiService: WineApiService
) {
    suspend fun identifyWine(fullPhotoJpeg: ByteArray): WineResult {
        val labelCrop = labelExtractor.extractLabelCrop(fullPhotoJpeg)
        return apiService.identifyWine(labelCrop)
    }
}

package com.winescanner.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class WineResult(
    @SerialName("wine_name") val wineName: String,
    @SerialName("public_rating") val publicRating: Float,
    @SerialName("guide_rating") val guideRating: Float,
    @SerialName("short_info") val shortInfo: String,
    @SerialName("gigachat_insights") val gigachatInsights: String
)


sealed interface ScanUiState {
    data class Camera(val errorMessage: String? = null) : ScanUiState
    data object Loading : ScanUiState
    data class Result(val wine: WineResult) : ScanUiState
}

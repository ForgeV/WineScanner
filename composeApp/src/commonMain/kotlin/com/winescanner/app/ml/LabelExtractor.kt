package com.winescanner.app.ml


data class LabelBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)


expect class LabelExtractor {
    suspend fun extractLabelCrop(photoBytes: ByteArray): ByteArray
}

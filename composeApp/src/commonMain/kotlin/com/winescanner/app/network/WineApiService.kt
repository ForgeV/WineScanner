package com.winescanner.app.network

import com.winescanner.app.model.WineResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess

class WineApiException(message: String) : Exception(message)


class WineApiService(
    private val client: HttpClient,
    private val baseUrl: String
) {
    suspend fun identifyWine(labelCropJpeg: ByteArray): WineResult {
        val response = client.submitFormWithBinaryData(
            url = "$baseUrl/api/v1/wines/identify",
            formData = formData {
                append(
                    key = "label",
                    value = labelCropJpeg,
                    headers = Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, "filename=\"label.jpg\"")
                    }
                )
            }
        )

        if (!response.status.isSuccess()) {
            throw WineApiException("Сервер вернул ошибку: ${response.status}")
        }


        return response.body()
    }
}

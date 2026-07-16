package com.winescanner.app.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object ApiConfig {
    // TODO: заменить на реальный адрес бэкенда хакатона перед демо.
    const val BASE_URL = "https://"
}


expect fun providePlatformEngine(): HttpClientEngine

object NetworkModule {
    fun createHttpClient(): HttpClient = HttpClient(providePlatformEngine()) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }
            )
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
        }
    }
}

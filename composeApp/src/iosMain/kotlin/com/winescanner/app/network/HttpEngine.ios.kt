package com.winescanner.app.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

actual fun providePlatformEngine(): HttpClientEngine = Darwin.create()

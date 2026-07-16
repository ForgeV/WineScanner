package com.winescanner.app.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

actual fun providePlatformEngine(): HttpClientEngine = OkHttp.create()

package org.example.project.core.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun createEngine(): HttpClientEngine = OkHttp.create()
package org.example.project.core.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal expect fun createEngine(): HttpClientEngine

fun createHttpClient(): HttpClient = HttpClient(createEngine()) {
    expectSuccess = true
    applyCommonConfig()
}

internal fun HttpClientConfig<*>.applyCommonConfig() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
    install(Logging) {
        logger = Logger.SIMPLE
        level = LogLevel.BODY
    }
    install(HttpTimeout){
        connectTimeoutMillis = 10_000
        requestTimeoutMillis = 30_000
    }
    defaultRequest{
        url("https://jsonplaceholder.typicode.com/")
        contentType(ContentType.Application.Json)
        header(HttpHeaders.Accept,"application/json")
    }
}

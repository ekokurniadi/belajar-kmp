package org.example.project.core.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.java.Java

internal actual fun createEngine(): HttpClientEngine = Java.create()
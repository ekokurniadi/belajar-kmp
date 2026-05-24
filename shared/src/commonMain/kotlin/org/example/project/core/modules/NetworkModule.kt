package org.example.project.core.modules

import org.example.project.core.network.createHttpClient
import org.koin.dsl.module

val networkModule= module {
    single { createHttpClient() }
}
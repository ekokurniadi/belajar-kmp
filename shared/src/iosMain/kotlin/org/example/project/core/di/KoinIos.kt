package org.example.project.core.di

import org.example.project.core.modules.appModules
import org.koin.core.context.startKoin

fun initKoinIOS(){
    startKoin {
        modules(appModules)
    }
}
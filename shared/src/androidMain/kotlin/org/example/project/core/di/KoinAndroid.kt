package org.example.project.core.di

import android.content.Context
import org.example.project.core.modules.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

fun initKoinAndroid(context: Context){
    startKoin {
        androidLogger()
        androidContext(context)
        modules(appModules)
    }
}
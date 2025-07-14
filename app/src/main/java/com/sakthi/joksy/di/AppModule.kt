package com.sakthi.joksy.di

import com.sakthi.joksy.data.JokeApiService
import com.sakthi.joksy.JoksyViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    single { HttpClient(Android) {
            install(ContentNegotiation) {
                json()
            }
        } }
    single { JokeApiService(get()) }
    viewModel { JoksyViewModel(get()) }

}
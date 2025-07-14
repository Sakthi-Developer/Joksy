package com.sakthi.joksy.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class JokeApiService(
    private val client: HttpClient
) {

    suspend fun getRandomJoke(): Joke {
        return client.get("https://official-joke-api.appspot.com/random_joke").body<Joke>()
    }
}
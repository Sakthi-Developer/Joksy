package com.sakthi.joksy

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakthi.joksy.data.Joke
import com.sakthi.joksy.data.JokeApiService
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class JoksyViewModel(
    private val jokeApiService: JokeApiService
) : ViewModel() {

    private val _jokeState = MutableStateFlow<JokeState>(JokeState.Loading)
    val jokeState = _jokeState.asStateFlow()

    fun getRandomJoke() {
        viewModelScope.launch {
            try {
                _jokeState.value = JokeState.Loading
                val joke = viewModelScope.async {
                    jokeApiService.getRandomJoke()
                }
                Log.d("JoksyViewModel", "getRandomJoke: ${joke.await().setup} ${joke.await().punchline}")
                _jokeState.value = JokeState.Success(joke.await())
            } catch (e: Exception) {
                _jokeState.value = JokeState.Error(e.message ?: "Unknown error")
            }
        }
    }

}

sealed class JokeState {
    object Loading : JokeState()
    data class Success(val joke: Joke) : JokeState()
    data class Error(val message: String) : JokeState()
}
package com.sakthi.joksy.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Joke(
    val id: Int,
    val type: String,
    val setup: String,
    val punchline: String
)

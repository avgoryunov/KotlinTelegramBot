package ru.avgoryunov.learnWordsBot.telegram.api.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CallbackQuery(
    @SerialName("message")
    val message: Message? = null,
    @SerialName("data")
    val data: String? = null,
)
package ru.avgoryunov.learnWordsBot.telegram.api.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessageResult(
    @SerialName("message_id")
    val messageId: Long? = null,
)
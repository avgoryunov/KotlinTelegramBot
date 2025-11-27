package ru.avgoryunov.learnWordsBot.telegram.api.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EditMessageRequest(
    @SerialName("chat_id")
    val chatId: Long,
    @SerialName("message_id")
    val messageId: Long,
    @SerialName("text")
    val text: String,
    @SerialName("reply_markup")
    val replyMarkup: ReplyMarkup? = null
)
package ru.avgoryunov.learnWordsBot.telegram.api.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EditMessageResponse(
    @SerialName("ok")
    val ok: Boolean,
    @SerialName("result")
    val result: MessageResult?,
)
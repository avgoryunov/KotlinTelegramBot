package ru.avgoryunov.learnWordsBot.telegram.api.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PhotoSize(
    @SerialName("file_id")
    val fileId: String,
)
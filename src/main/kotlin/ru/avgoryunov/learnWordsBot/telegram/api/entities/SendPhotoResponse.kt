package ru.avgoryunov.learnWordsBot.telegram.api.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SendPhotoResponse(
    @SerialName("result")
    val result: PhotoResult,
)
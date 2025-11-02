package ru.avgoryunov.learnWordsBot.telegram.api.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PhotoResult(
    @SerialName("photo")
    val photo: List<PhotoSize>,
)
package ru.avgoryunov.learnWordsBot.trainer.model

data class Word(
    val original: String,
    val translate: String,
    var correctAnswersCount: Int = 0,
)
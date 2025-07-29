package ru.avgoryunov.learnWordsBot.trainer.model

data class Question(
    val variants: List<Word>,
    val correctAnswer: Word,
)
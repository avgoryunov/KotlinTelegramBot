package ru.avgoryunov.learnWordsBot.trainer.model

import kotlin.text.filter
import kotlin.text.isLetterOrDigit

data class Question(
    val variants: List<Word>,
    val correctAnswer: Word,
) : IQuestion {
    override fun filterTheWord(string: String) : String {
        return string.filter { it.isLetterOrDigit() || it.isWhitespace() }
    }
}
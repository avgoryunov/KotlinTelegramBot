package ru.avgoryunov.learnWordsBot.trainer.model

interface IQuestion {
    fun filterTheWord(string: String) : String
}
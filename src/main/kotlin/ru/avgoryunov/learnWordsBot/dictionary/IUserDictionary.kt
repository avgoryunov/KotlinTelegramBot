package ru.avgoryunov.learnWordsBot.dictionary

import ru.avgoryunov.learnWordsBot.trainer.model.Word

interface IUserDictionary {
    fun getNumberOfLearnedWords(chatId: Long?): Int
    fun getSize(): Int
    fun getLearnedWords(chatId: Long?): List<Word>
    fun getUnlearnedWords(chatId: Long?): List<Word>
    fun setCorrectAnswersCount(chatId: Long?, original: String, correctAnswersCount: Int)
    fun resetUserProgress(chatId: Long?)
    fun addNewUser(userName: String, chatId: Long?)
}
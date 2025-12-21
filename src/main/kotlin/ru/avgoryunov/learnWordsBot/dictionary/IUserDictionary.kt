package ru.avgoryunov.learnWordsBot.dictionary

import ru.avgoryunov.learnWordsBot.trainer.model.Word

interface IUserDictionary {
    fun checkTheDatabaseStructure(): Boolean
    fun getNumberOfLearnedWords(chatId: Long?): Int
    fun getSize(): Int
    fun getLearnedWords(chatId: Long?): List<Word>
    fun getUnlearnedWords(chatId: Long?): List<Word>
    fun setCorrectAnswersCount(chatId: Long?, original: String, correctAnswersCount: Int)
    fun resetUserProgress(chatId: Long?)
    fun addNewUser(chatId: Long?, userName: String)
    fun updateTheDictionary(filename: String)
    fun getFilePath(text: Word): String?
    fun setFileId(text: Word, fileId: String?)
    fun getFileId(text: Word): String?
    fun getFilePathForEmptyPhoto(): String?
    fun setFileIdForEmptyPhoto(fileId: String?)
    fun getFileIdForEmptyPhoto(): String?
    fun setMessageId(chatId: Long?, messageId: Long?, messageIdColumn: MessageIdColumn)
    fun getMessageId(chatId: Long?, messageIdColumn: MessageIdColumn): Long?
}
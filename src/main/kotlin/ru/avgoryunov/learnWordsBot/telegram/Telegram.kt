package ru.avgoryunov.learnWordsBot.telegram

import kotlinx.serialization.json.Json.Default.decodeFromString
import ru.avgoryunov.learnWordsBot.dictionary.DatabaseUserDictionary
import ru.avgoryunov.learnWordsBot.telegram.api.entities.Response
import ru.avgoryunov.learnWordsBot.telegram.api.entities.Update
import ru.avgoryunov.learnWordsBot.telegram.api.TelegramBotService
import ru.avgoryunov.learnWordsBot.telegram.api.MENU_CLICKED
import ru.avgoryunov.learnWordsBot.telegram.api.STATISTICS_CLICKED
import ru.avgoryunov.learnWordsBot.telegram.api.RESET_CLICKED
import ru.avgoryunov.learnWordsBot.telegram.api.LEARN_WORDS_CLICKED
import ru.avgoryunov.learnWordsBot.telegram.api.CALLBACK_DATA_ANSWER_PREFIX
import ru.avgoryunov.learnWordsBot.telegram.api.entities.GetFileResponse
import ru.avgoryunov.learnWordsBot.telegram.api.handleUndoCommand
import ru.avgoryunov.learnWordsBot.telegram.api.handleUndoCommandMedia
import ru.avgoryunov.learnWordsBot.trainer.LearnWordsTrainer
import java.io.File

fun main(args: Array<String>) {

    val service = TelegramBotService(botToken = args[0])
    var lastUpdateId = 0L
    val trainer = LearnWordsTrainer()
    val dictionary = DatabaseUserDictionary()
    val check = dictionary.checkTheDatabaseStructure()

    if (!check) {
        println("Невозможно загрузить словарь")
        return
    }

    while (true) {
        Thread.sleep(2000)
        val responseString: String = service.getUpdates(lastUpdateId)

        println(responseString)

        val response: Response = service.json.decodeFromString(responseString)
        if (response.result.isEmpty()) continue
        val sortedUpdates = response.result.sortedBy { it.updateId }
        sortedUpdates.forEach { handleUpdates(it, service, trainer, dictionary) }
        lastUpdateId = sortedUpdates.last().updateId + 1
    }
}

fun handleUpdates(
    update: Update,
    service: TelegramBotService,
    trainer: LearnWordsTrainer,
    dictionary: DatabaseUserDictionary,
) {
    val message = update.message?.text

    println("Пользовательское сообщение: $message")

    val chatId = update.message?.chat?.id ?: update.callbackQuery?.message?.chat?.id ?: return
    val userName = update.message?.chat?.userName ?: update.callbackQuery?.message?.chat?.userName ?: return
    val data = update.callbackQuery?.data
    val document = update.message?.document

    if (document != null) {
        val jsonResponse = service.getFile(document.fileId)
        val response: GetFileResponse = decodeFromString(jsonResponse)
        response.result?.let {
            val filename = "${it.fileUniqueId}.${it.filePath.substringAfter(".")}"
            if (!File(filename).exists()) {
                service.downloadFile(it.filePath, filename)
                dictionary.updateTheDictionary(filename)
                println("Словарь пополнен")
            }
        }
    }

    if (message?.lowercase() == WELCOME_MESSAGE) {
        val text = "Hello"
        service.sendMessage(chatId, text, replyMarkup = null)
    }

    if (message?.lowercase() == ROLLBACK_TO_PREVIOUS) {
        handleUndoCommand(chatId, service)
        handleUndoCommandMedia(chatId, service)
    }

    if (message?.lowercase() == ProgramStart.MENU || message?.lowercase() == ProgramStart.START || data?.lowercase() == MENU_CLICKED) {
        service.sendMenu(chatId)
        dictionary.addNewUser(chatId, userName)
    }

    if (data?.lowercase() == STATISTICS_CLICKED) {
        val statistics = trainer.getStatistics(chatId, dictionary)
        service.sendProgress(chatId, statistics, dictionary)
    }

    if (data?.lowercase() == RESET_CLICKED) {
        dictionary.resetUserProgress(chatId)
        val text = "Прогресс сброшен"
        service.sendMessage(chatId, text, replyMarkup = null)
    }

    if (data?.lowercase() == LEARN_WORDS_CLICKED) {
        val nextQuestion = service.checkNextQuestion(chatId, trainer, dictionary)
        if (nextQuestion != null) {
            service.sendQuestion(chatId, nextQuestion, dictionary)
            service.checkPhotoAndSend(chatId, nextQuestion, dictionary)
        }
    }

    if (data?.startsWith(CALLBACK_DATA_ANSWER_PREFIX) == true) {
        val question = trainer.question
        val userAnswerIndex = data.substringAfter(CALLBACK_DATA_ANSWER_PREFIX).toInt()
        val answerIsCorrect = trainer.checkAnswer(chatId, question, userAnswerIndex, dictionary)

        if (question != null) service.showAnswerStatus(chatId, question, answerIsCorrect, dictionary)

        if (answerIsCorrect) service.updateProgress(chatId, trainer, dictionary)

        val nextQuestion = service.checkNextQuestion(chatId, trainer, dictionary)

        if (nextQuestion != null) {
            service.updateQuestion(chatId, nextQuestion, dictionary)
            service.safeEditMessageMedia(chatId, nextQuestion, dictionary)
        }
    }
}

object ProgramStart {
    const val MENU = "menu"
    const val START = "/start"
}

const val WELCOME_MESSAGE = "hello"
const val ROLLBACK_TO_PREVIOUS = "/undo"
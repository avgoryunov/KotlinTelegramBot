package ru.avgoryunov.learnWordsBot.telegram

import ru.avgoryunov.learnWordsBot.dictionary.DatabaseUserDictionary
import ru.avgoryunov.learnWordsBot.telegram.api.entities.Response
import ru.avgoryunov.learnWordsBot.telegram.api.entities.Update
import ru.avgoryunov.learnWordsBot.telegram.api.TelegramBotService
import ru.avgoryunov.learnWordsBot.telegram.api.MENU_CLICKED
import ru.avgoryunov.learnWordsBot.telegram.api.STATISTICS_CLICKED
import ru.avgoryunov.learnWordsBot.telegram.api.RESET_CLICKED
import ru.avgoryunov.learnWordsBot.telegram.api.LEARN_WORDS_CLICKED
import ru.avgoryunov.learnWordsBot.telegram.api.CALLBACK_DATA_ANSWER_PREFIX
import ru.avgoryunov.learnWordsBot.trainer.LearnWordsTrainer

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

    if (message?.lowercase() == WELCOME_MESSAGE) {
        val message = "Hello"
        service.sendMessage(chatId, message)
    }

    if (message?.lowercase() == ProgramStart.MENU || message?.lowercase() == ProgramStart.START || data?.lowercase() == MENU_CLICKED) {
        service.sendMenu(chatId)
        dictionary.addNewUser(userName, chatId)
    }

    if (data?.lowercase() == STATISTICS_CLICKED) {
        val statistics = trainer.getStatistics(chatId, dictionary)
        val message =
            if (statistics != null) "Выучено ${statistics.numberOfLearnedWords} из " +
                    "${statistics.numberOfTotalWords} слов | ${statistics.percentOfLearnedWords}%"
            else "Отсутствуют слова в словаре"
        service.sendMessage(chatId, message)
    }

    if (data?.lowercase() == RESET_CLICKED) {
        dictionary.resetUserProgress(chatId)
        val message = "Прогресс сброшен"
        service.sendMessage(chatId, message)
    }

    if (data?.lowercase() == LEARN_WORDS_CLICKED) {
        service.checkNextQuestionAndSend(chatId, dictionary, trainer)
    }

    if (data?.startsWith(CALLBACK_DATA_ANSWER_PREFIX) == true) {
        val question = trainer.question
        val userAnswerIndex = data.substringAfter(CALLBACK_DATA_ANSWER_PREFIX).toInt()
        val message =
            if (trainer.checkAnswer(chatId, question, userAnswerIndex, dictionary)) "\'Правильно!\'"
            else "\'Неправильно! ${trainer.question?.correctAnswer?.original} - это ${trainer.question?.correctAnswer?.translate}\'"
        service.sendMessage(chatId, message)
        service.checkNextQuestionAndSend(chatId, dictionary, trainer)
    }
}

object ProgramStart {
    const val MENU = "menu"
    const val START = "/start"
}

const val WELCOME_MESSAGE = "hello"
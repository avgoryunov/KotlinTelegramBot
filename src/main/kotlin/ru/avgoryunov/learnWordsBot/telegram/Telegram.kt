package ru.avgoryunov.learnWordsBot.telegram

import ru.avgoryunov.learnWordsBot.telegram.api.TelegramBotService
import ru.avgoryunov.learnWordsBot.trainer.LearnWordsTrainer
import ru.avgoryunov.learnWordsBot.telegram.api.entities.Response
import ru.avgoryunov.learnWordsBot.telegram.api.entities.Update
import ru.avgoryunov.learnWordsBot.telegram.api.MENU_CLICKED
import ru.avgoryunov.learnWordsBot.telegram.api.STATISTICS_CLICKED
import ru.avgoryunov.learnWordsBot.telegram.api.RESET_CLICKED
import ru.avgoryunov.learnWordsBot.telegram.api.LEARN_WORDS_CLICKED
import ru.avgoryunov.learnWordsBot.telegram.api.CALLBACK_DATA_ANSWER_PREFIX

fun main(args: Array<String>) {

    val telegramBotService = TelegramBotService(args[0])
    var lastUpdateId: Long = 0

    val trainers = try {
        HashMap<Long, LearnWordsTrainer>()
    } catch (e: Exception) {
        println("Невозможно загрузить словарь: ${e.message}")
        return
    }

    while (true) {
        Thread.sleep(2000)
        val responseString: String = telegramBotService.getUpdates(lastUpdateId)

        println(responseString)

        val response: Response = telegramBotService.json.decodeFromString(responseString)
        if (response.result.isEmpty()) continue
        val sortedUpdates = response.result.sortedBy { it.updateId }
        sortedUpdates.forEach { handleUpdates(it, telegramBotService, trainers) }
        lastUpdateId = sortedUpdates.last().updateId + 1
    }
}

fun handleUpdates(
    update: Update,
    telegramBotService: TelegramBotService,
    trainers: HashMap<Long, LearnWordsTrainer>
) {

    val message = update.message?.text

    println("userText: $message")

    val chatId = update.message?.chat?.id ?: update.callbackQuery?.message?.chat?.id ?: return
    val data = update.callbackQuery?.data
    val trainer = trainers.getOrPut(chatId) { LearnWordsTrainer("$chatId.txt") }

    if (message?.lowercase() == WELCOME_MESSAGE) {
        telegramBotService.sendMessage(chatId, "Hello")
    }

    if (message?.lowercase() == ProgramStart.MENU || message?.lowercase() == ProgramStart.START || data?.lowercase() == MENU_CLICKED) {
        telegramBotService.sendMenu(chatId)
    }

    if (data?.lowercase() == STATISTICS_CLICKED) {
        val statistics = trainer.getStatistics()
        telegramBotService.sendMessage(
            chatId,
            "Выучено ${statistics.learned} из ${statistics.total} слов | ${statistics.percentLearned}%"
        )
    }

    if (data?.lowercase() == RESET_CLICKED) {
        trainer.resetProgress()
        telegramBotService.sendMessage(chatId, "Прогресс сброшен")
    }

    if (data?.lowercase() == LEARN_WORDS_CLICKED) {
        telegramBotService.checkNextQuestionAndSend(trainer, telegramBotService, chatId)
    }

    if (data?.startsWith(CALLBACK_DATA_ANSWER_PREFIX) == true) {
        val userAnswerIndex = data.substringAfter(CALLBACK_DATA_ANSWER_PREFIX).toInt()
        if (trainer.checkAnswer(userAnswerIndex)) telegramBotService.sendMessage(chatId, "\'Правильно!\'")
        else {
            telegramBotService.sendMessage(
                chatId,
                "\'Неправильно! ${trainer.question?.correctAnswer?.original} - это ${trainer.question?.correctAnswer?.translate}\'"
            )
        }
        telegramBotService.checkNextQuestionAndSend(trainer, telegramBotService, chatId)
    }
}

object ProgramStart {
    const val MENU = "menu"
    const val START = "/start"
}

const val WELCOME_MESSAGE = "hello"
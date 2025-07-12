package org.example

fun main(args: Array<String>) {

    val telegramBotService = TelegramBotService(
        args[0],
    )
    var lastUpdateId: Long = 0
    val updateIdRegex: Regex = "\"update_id\":(\\d+)".toRegex()
    val messageTextRegex: Regex = "\"text\":\"(.+?)\"".toRegex()
    val chatIdRegex: Regex = """"chat":\s*\{\s*"id":\s*(-?\d+)""".toRegex()
    val dataRegex: Regex = "\"data\":\"(.+?)\"".toRegex()

    val trainer = try {
        LearnWordsTrainer()
    } catch (e: Exception) {
        println("Невозможно загрузить словарь: ${e.message}")
        return
    }

    while (true) {
        Thread.sleep(2000)
        val updates: String = telegramBotService.getUpdates(lastUpdateId)

        println(updates)

        val updateId = updateIdRegex.find(updates)?.groups?.get(1)?.value?.toLongOrNull() ?: continue
        lastUpdateId = updateId + 1

        val message = messageTextRegex.find(updates)?.groups?.get(1)?.value

        println("userText: $message")

        val chatId = chatIdRegex.find(updates)?.groups?.get(1)?.value?.toLongOrNull() ?: continue
        val data = dataRegex.find(updates)?.groups?.get(1)?.value

        if (message?.lowercase() == WELCOME_MESSAGE) {
            val message = telegramBotService.sendMessage(chatId, "Hello")
            println(message)
            val botMessage = messageTextRegex.find(message)?.groups?.get(1)?.value

            println("botText: $botMessage")
        }

        if (message?.lowercase() == ProgramStart.MENU) {
            telegramBotService.sendMenu(chatId)
        }

        if (message?.lowercase() == ProgramStart.START) {
            telegramBotService.sendMenu(chatId)
        }

        if (data?.lowercase() == STATISTICS_CLICKED) {
            val statistics = trainer.getStatistics()
            telegramBotService.sendMessage(
                chatId,
                "Выучено ${statistics.learned} из ${statistics.total} слов | ${statistics.percentLearned}%"
            )
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
}

object ProgramStart {
    const val MENU = "menu"
    const val START = "/start"
}

const val WELCOME_MESSAGE = "hello"
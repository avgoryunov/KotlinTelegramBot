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

        if (message?.lowercase() == ProgramStart.VALUE_ONE) {
            telegramBotService.sendMenu(chatId)
        }

        if (message?.lowercase() == ProgramStart.VALUE_TWO) {
            telegramBotService.sendMenu(chatId)
        }

        if (data?.lowercase() == STATISTICS_CLICKED) {
            telegramBotService.sendMessage(chatId, "Выучено 10 из 10 слов | 100%")
        }
    }
}

object ProgramStart {
    const val VALUE_ONE = "menu"
    const val VALUE_TWO = "/start"
}

const val WELCOME_MESSAGE = "hello"
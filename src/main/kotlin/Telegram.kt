package org.example

fun main(args: Array<String>) {

    val telegramBotService = TelegramBotService(
        args[0],
        0,
        "Hello",
    )

    while (true) {
        Thread.sleep(2000)
        val updates: String = telegramBotService.getUpdates(
            botToken = telegramBotService.botToken,
            updateId = telegramBotService.updateId,
        )

        println(updates)

        val startUpdateId = updates.lastIndexOf("update_id")
        val endUpdateId = updates.lastIndexOf(",\n\"message\"")
        if (startUpdateId == -1 || endUpdateId == -1) continue
        val updateIdString = updates.substring(startUpdateId + 11, endUpdateId)
        telegramBotService.updateId = updateIdString.toInt() + 1

        val userTextRegex = "\"text\":\"(.+?)\"".toRegex()
        val userTextMatchResult: MatchResult? = userTextRegex.find(updates)
        val userTextGroups = userTextMatchResult?.groups
        val userText = userTextGroups?.get(1)?.value

        println("userText: $userText")

        val chatIdRegex: Regex = """"chat":\s*\{\s*"id":\s*(-?\d+)""".toRegex()
        val chatIdMatchResult: MatchResult? = chatIdRegex.find(updates)
        val chatIdGroups = chatIdMatchResult?.groups
        val chatId = chatIdGroups?.get(1)?.value

        if (userText?.lowercase() == "Hello".lowercase()) {
            val message: String = telegramBotService.sendMessage(
                botToken = telegramBotService.botToken,
                chatId = chatId,
                text = telegramBotService.botText,
            )
            println(message)
            println("botText: ${telegramBotService.botText}")
        }
    }
}
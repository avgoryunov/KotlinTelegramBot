package org.example

fun main(args: Array<String>) {

    val telegramBotService = TelegramBotService(
        args[0],
    )
    var updateId = 0
    val textRegex: Regex = "\"text\":\"(.+?)\"".toRegex()
    val chatIdRegex: Regex = """"chat":\s*\{\s*"id":\s*(-?\d+)""".toRegex()

    while (true) {
        Thread.sleep(2000)
        val updates: String = telegramBotService.getUpdates(
            updateId = updateId,
        )

        println(updates)

        val startUpdateId = updates.lastIndexOf("update_id")
        val endUpdateId = updates.lastIndexOf(",\n\"message\"")
        if (startUpdateId == -1 || endUpdateId == -1) continue
        val updateIdString = updates.substring(startUpdateId + 11, endUpdateId)
        updateId = updateIdString.toInt() + 1

        val userTextMatchResult: MatchResult? = textRegex.find(updates)
        val userTextGroups = userTextMatchResult?.groups
        val userText = userTextGroups?.get(1)?.value

        println("userText: $userText")

        val chatIdMatchResult: MatchResult? = chatIdRegex.find(updates)
        val chatIdGroups = chatIdMatchResult?.groups
        val chatId = chatIdGroups?.get(1)?.value

        if (userText?.lowercase() == "Hello".lowercase()) {
            val message: String = telegramBotService.sendMessage(
                chatId = chatId,
                text = "Hello",
            )

            println(message)

            val botTextMatchResult: MatchResult? = textRegex.find(message)
            val botTextGroups = botTextMatchResult?.groups
            val botText = botTextGroups?.get(1)?.value

            println("botText: $botText")
        }
    }
}
package ru.avgoryunov.learnWordsBot.telegram.api

import ru.avgoryunov.learnWordsBot.telegram.api.entities.ReplyMarkup

data class MessageEditor(
    val messageId: Long,
    val text: String,
    val replyMarkup: ReplyMarkup? = null,
)

val userHistories = mutableMapOf<Long, MutableList<MessageEditor>>()

fun saveMessageStateForUser(chatId: Long, messageId: Long, text: String, replyMarkup: ReplyMarkup?) {
    val history = userHistories.getOrPut(chatId) { mutableListOf() }
    history.add(MessageEditor(messageId, text, replyMarkup))

    if (history.size > 20) history.removeAt(0)
}

fun handleUndoCommand(chatId: Long, service: TelegramBotService) {
    val history = userHistories[chatId] ?: return

    if (history.size < 2) {
        println("Нет предыдущих сообщений")
        return
    }

    history.removeAt(history.lastIndex)
    val prevState = history.last()

    service.safeEditMessageText(
        chatId = chatId,
        messageId = prevState.messageId,
        newText = prevState.text,
        replyMarkup = prevState.replyMarkup,
    )
}
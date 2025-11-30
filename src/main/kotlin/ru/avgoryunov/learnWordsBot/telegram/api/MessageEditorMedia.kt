package ru.avgoryunov.learnWordsBot.telegram.api

data class MessageEditorMedia(
    val messageId: Long,
    val fileId: String?,
    val hasSpoiler: Boolean = false,
)

val userHistoriesMedia = mutableMapOf<Long, MutableList<MessageEditorMedia>>()

fun saveMessageStateForUser(chatId: Long, messageId: Long, fileId: String?) {
    val history = userHistoriesMedia.getOrPut(chatId) { mutableListOf() }
    history.add(MessageEditorMedia(messageId, fileId))

    if (history.size > 20) history.removeAt(0)
}

fun handleUndoCommandMedia(chatId: Long, service: TelegramBotService) {
    val history = userHistoriesMedia[chatId] ?: return

    if (history.size < 2) {
        println("Нет предыдущих сообщений")
        return
    }

    history.removeAt(history.lastIndex)
    val prevState = history.last()

    service.editMessageMedia(
        chatId = chatId,
        messageId = prevState.messageId,
        fileId = prevState.fileId,
        hasSpoiler = prevState.hasSpoiler,
    )
}
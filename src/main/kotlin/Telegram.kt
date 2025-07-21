package org.example

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Update(
    @SerialName("update_id")
    val updateId: Long,
    @SerialName("message")
    val message: Message? = null,
    @SerialName("callback_query")
    val callbackQuery: CallbackQuery? = null,
)

@Serializable
data class Response(
    @SerialName("result")
    val result: List<Update>,
)

@Serializable
data class Message(
    @SerialName("text")
    val text: String,
    @SerialName("chat")
    val chat: Chat,
)

@Serializable
data class CallbackQuery(
    @SerialName("data")
    val data: String? = null,
    @SerialName("message")
    val message: Message? = null,
)

@Serializable
data class Chat(
    @SerialName("id")
    val id: Long,
)

@Serializable
data class SendMessageRequest(
    @SerialName("chat_id")
    val chatId: Long?,
    @SerialName("text")
    val text: String,
    @SerialName("reply_markup")
    val replyMarkup: ReplyMarkup? = null,
)

@Serializable
data class ReplyMarkup(
    @SerialName("inline_keyboard")
    val inlineKeyboard: List<List<InlineKeyboard>>,
)

@Serializable
data class InlineKeyboard(
    @SerialName("callback_data")
    val callbackData: String,
    @SerialName("text")
    val text: String,
)

fun main(args: Array<String>) {

    val telegramBotService = TelegramBotService(
        args[0],
    )
    var lastUpdateId: Long = 0

    val json = Json {
        ignoreUnknownKeys = true
    }

    val trainer = try {
        LearnWordsTrainer()
    } catch (e: Exception) {
        println("Невозможно загрузить словарь: ${e.message}")
        return
    }

    while (true) {
        Thread.sleep(2000)
        val responseString: String = telegramBotService.getUpdates(lastUpdateId)

        println(responseString)

        val response: Response = json.decodeFromString(responseString)
        val updates = response.result
        val firstUpdate = updates.firstOrNull() ?: continue
        val updateId = firstUpdate.updateId
        lastUpdateId = updateId + 1

        val message = firstUpdate.message?.text

        println("userText: $message")

        val chatId = firstUpdate.message?.chat?.id ?: firstUpdate.callbackQuery?.message?.chat?.id
        val data = firstUpdate.callbackQuery?.data

        if (message?.lowercase() == WELCOME_MESSAGE) {
            telegramBotService.sendMessage(json, chatId, "Hello")
        }

        if (message?.lowercase() == ProgramStart.MENU) {
            telegramBotService.sendMenu(json, chatId)
        }

        if (message?.lowercase() == ProgramStart.START) {
            telegramBotService.sendMenu(json, chatId)
        }

        if (data?.lowercase() == STATISTICS_CLICKED) {
            val statistics = trainer.getStatistics()
            telegramBotService.sendMessage(
                json,
                chatId,
                "Выучено ${statistics.learned} из ${statistics.total} слов | ${statistics.percentLearned}%"
            )
        }

        if (data?.lowercase() == LEARN_WORDS_CLICKED) {
            telegramBotService.checkNextQuestionAndSend(trainer, telegramBotService, chatId)
        }

        if (data?.lowercase() == EXIT_CLICKED) {
            return
        }

        if (data?.startsWith(CALLBACK_DATA_ANSWER_PREFIX) == true) {
            val userAnswerIndex = data.substringAfter(CALLBACK_DATA_ANSWER_PREFIX).toInt()
            if (trainer.checkAnswer(userAnswerIndex)) telegramBotService.sendMessage(json, chatId, "\'Правильно!\'")
            else {
                telegramBotService.sendMessage(
                    json,
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
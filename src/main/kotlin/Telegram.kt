package org.example

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.String

@Serializable
data class Response(
    @SerialName("result")
    val result: List<Update>,
)

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
data class Message(
    @SerialName("chat")
    val chat: Chat,
    @SerialName("text")
    val text: String,
)

@Serializable
data class CallbackQuery(
    @SerialName("message")
    val message: Message? = null,
    @SerialName("data")
    val data: String? = null,
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
    @SerialName("text")
    val text: String,
    @SerialName("callback_data")
    val callbackData: String,
)

fun main(args: Array<String>) {

    val telegramBotService = TelegramBotService(args[0])
    var lastUpdateId: Long = 0
    val json = Json { ignoreUnknownKeys = true }

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

        val response: Response = json.decodeFromString(responseString)
        if (response.result.isEmpty()) continue
        val sortedUpdates = response.result.sortedBy { it.updateId }
        sortedUpdates.forEach { handleUpdates(it, json, telegramBotService, trainers) }
        lastUpdateId = sortedUpdates.last().updateId + 1
    }
}

fun handleUpdates(
    update: Update,
    json: Json,
    telegramBotService: TelegramBotService,
    trainers: HashMap<Long, LearnWordsTrainer>
) {

    val message = update.message?.text

    println("userText: $message")

    val chatId = update.message?.chat?.id ?: update.callbackQuery?.message?.chat?.id ?: return
    val data = update.callbackQuery?.data
    val trainer = trainers.getOrPut(chatId) { LearnWordsTrainer("$chatId.txt") }

    if (message?.lowercase() == WELCOME_MESSAGE) {
        telegramBotService.sendMessage(json, chatId, "Hello")
    }

    if (message?.lowercase() == ProgramStart.MENU || message?.lowercase() == ProgramStart.START || data?.lowercase() == MENU_CLICKED) {
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

    if (data?.lowercase() == RESET_CLICKED) {
        trainer.resetProgress()
        telegramBotService.sendMessage(json, chatId, "Прогресс сброшен")
    }

    if (data?.lowercase() == LEARN_WORDS_CLICKED) {
        telegramBotService.checkNextQuestionAndSend(trainer, telegramBotService, chatId)
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

object ProgramStart {
    const val MENU = "menu"
    const val START = "/start"
}

const val WELCOME_MESSAGE = "hello"
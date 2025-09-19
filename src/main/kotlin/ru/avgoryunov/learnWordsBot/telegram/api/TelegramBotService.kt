package ru.avgoryunov.learnWordsBot.telegram.api

import kotlinx.serialization.json.Json
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.URI
import java.net.http.HttpResponse
import ru.avgoryunov.learnWordsBot.dictionary.DatabaseUserDictionary
import ru.avgoryunov.learnWordsBot.telegram.api.entities.SendMessageRequest
import ru.avgoryunov.learnWordsBot.telegram.api.entities.ReplyMarkup
import ru.avgoryunov.learnWordsBot.telegram.api.entities.InlineKeyboard
import ru.avgoryunov.learnWordsBot.trainer.LearnWordsTrainer
import ru.avgoryunov.learnWordsBot.trainer.model.Question
import java.io.IOException

class TelegramBotService(
    val botToken: String,
    val json: Json = Json { ignoreUnknownKeys = true },
) {
    val client: HttpClient = HttpClient.newBuilder().build()

    fun getUpdates(updateId: Long): String {
        val urlGetUpdates = "$TELEGRAM_ADDRESS$botToken/getUpdates?offset=$updateId"
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
        val response: HttpResponse<String> =
            try {
                client.send(request, HttpResponse.BodyHandlers.ofString())
            } catch (e: IOException) {
                if (e.message?.contains("GOAWAY") == true) {
                    client.send(request, HttpResponse.BodyHandlers.ofString())
                } else throw e
            }
        return response.body()
    }

    fun sendMessage(chatId: Long?, message: String): String? {
        val urlSendMessage = "$TELEGRAM_ADDRESS$botToken/sendMessage"
        val requestBody = SendMessageRequest(chatId, message)
        val requestBodyString = json.encodeToString(requestBody)
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
            .build()
        val response: HttpResponse<String>? =
            try {
                client.send(request, HttpResponse.BodyHandlers.ofString())
            } catch (e: IOException) {
                if (e.message?.contains("GOAWAY") == true) {
                    client.send(request, HttpResponse.BodyHandlers.ofString())
                } else {
                    println(e.message)
                    null
                }
            }
        return response?.body()
    }

    fun sendMenu(chatId: Long?): String? {
        val urlSendMessage = "$TELEGRAM_ADDRESS$botToken/sendMessage"
        val requestBody = SendMessageRequest(
            chatId = chatId,
            text = "Основное меню",
            replyMarkup = ReplyMarkup(
                listOf(
                    listOf(
                        InlineKeyboard(
                            text = "Изучать слова",
                            callbackData = LEARN_WORDS_CLICKED
                        ),
                        InlineKeyboard(
                            text = "Статистика",
                            callbackData = STATISTICS_CLICKED
                        ),
                    ),
                    listOf(
                        InlineKeyboard(
                            text = "Сбросить прогресс",
                            callbackData = RESET_CLICKED
                        ),
                    )
                )
            ),
        )
        val requestBodyString = json.encodeToString(requestBody)
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
            .build()
        val response: HttpResponse<String>? =
            try {
                client.send(request, HttpResponse.BodyHandlers.ofString())
            } catch (e: IOException) {
                if (e.message?.contains("GOAWAY") == true) {
                    client.send(request, HttpResponse.BodyHandlers.ofString())
                } else {
                    println(e.message)
                    null
                }
            }
        return response?.body()
    }

    fun checkNextQuestionAndSend(
        chatId: Long?,
        dictionary: DatabaseUserDictionary,
        trainer: LearnWordsTrainer,
    ) {
        val question = trainer.getNextQuestion(chatId, dictionary)

        if (question == null) {
            val message = "Все слова в словаре выучены"
            sendMessage(chatId, message)
        } else sendQuestion(chatId, question)
    }

    fun sendQuestion(chatId: Long?, question: Question): String? {
        val urlSendMessage = "$TELEGRAM_ADDRESS$botToken/sendMessage"
        val requestBody = SendMessageRequest(
            chatId = chatId,
            text = question.correctAnswer.original,
            replyMarkup = ReplyMarkup(
                listOf(
                    question.variants.mapIndexed { index, word ->
                        InlineKeyboard(
                            text = word.translate,
                            callbackData = "$CALLBACK_DATA_ANSWER_PREFIX$index"
                        )
                    },
                    listOf(
                        InlineKeyboard(
                            text = "Возврат в меню",
                            callbackData = MENU_CLICKED
                        ),
                    )
                )
            )
        )
        val requestBodyString = json.encodeToString(requestBody)
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
            .build()
        val response: HttpResponse<String>? =
            try {
                client.send(request, HttpResponse.BodyHandlers.ofString())
            } catch (e: IOException) {
                if (e.message?.contains("GOAWAY") == true) {
                    client.send(request, HttpResponse.BodyHandlers.ofString())
                } else {
                    println(e.message)
                    null
                }
            }
        return response?.body()
    }
}

const val TELEGRAM_ADDRESS = "https://api.telegram.org/bot"
const val LEARN_WORDS_CLICKED = "learn_words_clicked"
const val STATISTICS_CLICKED = "statistics_clicked"
const val RESET_CLICKED = "reset_clicked"
const val MENU_CLICKED = "menu_clicked"
const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"
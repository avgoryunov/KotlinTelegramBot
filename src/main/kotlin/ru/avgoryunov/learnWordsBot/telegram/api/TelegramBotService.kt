package ru.avgoryunov.learnWordsBot.telegram.api

import kotlinx.serialization.json.Json
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.URI
import java.net.http.HttpResponse
import ru.avgoryunov.learnWordsBot.dictionary.DatabaseUserDictionary
import ru.avgoryunov.learnWordsBot.telegram.api.entities.GetFileRequest
import ru.avgoryunov.learnWordsBot.telegram.api.entities.SendMessageRequest
import ru.avgoryunov.learnWordsBot.telegram.api.entities.ReplyMarkup
import ru.avgoryunov.learnWordsBot.telegram.api.entities.InlineKeyboard
import ru.avgoryunov.learnWordsBot.trainer.LearnWordsTrainer
import ru.avgoryunov.learnWordsBot.trainer.model.Question
import java.io.File
import java.io.IOException
import java.io.InputStream

class TelegramBotService(
    val botToken: String,
    val json: Json = Json { ignoreUnknownKeys = true },
) {
    val client: HttpClient = HttpClient.newBuilder().build()

    fun getUpdates(updateId: Long): String {
        val urlGetUpdates = "$BOT_URL$botToken/getUpdates?offset=$updateId"
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
        val urlSendMessage = "$BOT_URL$botToken/sendMessage"
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
        val urlSendMessage = "$BOT_URL$botToken/sendMessage"
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
        val urlSendMessage = "$BOT_URL$botToken/sendMessage"
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

    fun getFile(fileId: String): String {
        val urlGetFile = "$BOT_URL$botToken/getFile"
        val requestBody = GetFileRequest(fileId)
        val requestBodyString = json.encodeToString(requestBody)
        val client: HttpClient = HttpClient.newBuilder().build()
        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(URI.create(urlGetFile))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
            .build()
        val response: HttpResponse<String> = client.send(
            request,
            HttpResponse.BodyHandlers.ofString()
        )
        return response.body()
    }

    fun downloadFile(filePath: String, fileName: String) {
        val urlGetFile = "$BOT_FILE_URL$botToken/$filePath"
        println(urlGetFile)
        val request = HttpRequest
            .newBuilder()
            .uri(URI.create(urlGetFile))
            .GET()
            .build()

        val response: HttpResponse<InputStream> = HttpClient
            .newHttpClient()
            .send(request, HttpResponse.BodyHandlers.ofInputStream())

        println("status code: " + response.statusCode())
        val body: InputStream = response.body()
        body.copyTo(File(fileName).outputStream(), 16 * 1024)
    }
}

const val BOT_URL = "https://api.telegram.org/bot"
const val BOT_FILE_URL = "https://api.telegram.org/file/bot"
const val LEARN_WORDS_CLICKED = "learn_words_clicked"
const val STATISTICS_CLICKED = "statistics_clicked"
const val RESET_CLICKED = "reset_clicked"
const val MENU_CLICKED = "menu_clicked"
const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"
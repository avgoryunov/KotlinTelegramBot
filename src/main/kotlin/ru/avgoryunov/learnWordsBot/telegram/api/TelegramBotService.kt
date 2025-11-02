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
import ru.avgoryunov.learnWordsBot.telegram.api.entities.SendPhotoResponse
import ru.avgoryunov.learnWordsBot.trainer.LearnWordsTrainer
import ru.avgoryunov.learnWordsBot.trainer.model.Question
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Random

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
        } else {
            var fileId = dictionary.checkForFileIdAvailability(question.correctAnswer)
            val filePath = dictionary.checkForFilePathAvailability(question.correctAnswer)

            if (filePath != null) {
                val photoResponse = sendPhoto(chatId, fileId, File(filePath))

                if (fileId == null) {
                    val sendPhotoresponse = json.decodeFromString<SendPhotoResponse>(photoResponse)
                    val lastPhotoIndex = sendPhotoresponse.result.photo.lastIndex
                    fileId = sendPhotoresponse.result.photo.getOrNull(lastPhotoIndex)?.fileId
                    dictionary.saveFileIdToTheDictionary(question.correctAnswer, fileId)
                }
            }
            sendQuestion(chatId, question)
        }
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

    fun sendPhoto(chatId: Long?, fileId: String?, file: File, hasSpoiler: Boolean = false): String {
        val data: MutableMap<String, Any> = LinkedHashMap()
        data["chat_id"] = chatId.toString()
        data["photo"] = fileId ?: file
        data["has_spoiler"] = hasSpoiler
        val boundary: String = BigInteger(35, Random()).toString()
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$BOT_URL$botToken/sendPhoto"))
            .postMultipartFormData(boundary, data)
            .build()
        val client: HttpClient = HttpClient.newBuilder().build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    private fun HttpRequest.Builder.postMultipartFormData(
        boundary: String,
        data: Map<String, Any>,
    ): HttpRequest.Builder {
        val byteArrays = ArrayList<ByteArray>()
        val separator = "--$boundary\r\nContent-Disposition: form-data; name=".toByteArray(StandardCharsets.UTF_8)

        for (entry in data.entries) {
            byteArrays.add(separator)
            when (entry.value) {
                is File -> {
                    val file = entry.value as File
                    val path = Path.of(file.toURI())
                    val mimeType = Files.probeContentType(path)
                    byteArrays.add(
                        "${entry.key}; filename=${path.fileName}\r\nContent-Type: $mimeType\r\n\r\n".toByteArray(
                            StandardCharsets.UTF_8
                        )
                    )
                    byteArrays.add(Files.readAllBytes(path))
                    byteArrays.add("\r\n".toByteArray(StandardCharsets.UTF_8))
                }

                else -> byteArrays.add("${entry.key}\r\n\r\n${entry.value}\r\n".toByteArray(StandardCharsets.UTF_8))
            }
        }
        byteArrays.add("--$boundary--".toByteArray(StandardCharsets.UTF_8))

        this.header("Content-Type", "multipart/form-data; boundary=$boundary")
            .POST(HttpRequest.BodyPublishers.ofByteArrays(byteArrays))
        return this
    }
}

const val BOT_URL = "https://api.telegram.org/bot"
const val BOT_FILE_URL = "https://api.telegram.org/file/bot"
const val LEARN_WORDS_CLICKED = "learn_words_clicked"
const val STATISTICS_CLICKED = "statistics_clicked"
const val RESET_CLICKED = "reset_clicked"
const val MENU_CLICKED = "menu_clicked"
const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"
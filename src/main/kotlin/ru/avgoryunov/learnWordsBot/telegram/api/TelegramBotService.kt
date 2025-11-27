package ru.avgoryunov.learnWordsBot.telegram.api

import kotlinx.serialization.json.Json
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.URI
import java.net.http.HttpResponse
import ru.avgoryunov.learnWordsBot.dictionary.DatabaseUserDictionary
import ru.avgoryunov.learnWordsBot.telegram.api.entities.DeleteMessage
import ru.avgoryunov.learnWordsBot.telegram.api.entities.EditMessageRequest
import ru.avgoryunov.learnWordsBot.telegram.api.entities.EditMessageResponse
import ru.avgoryunov.learnWordsBot.telegram.api.entities.GetFileRequest
import ru.avgoryunov.learnWordsBot.telegram.api.entities.SendMessageRequest
import ru.avgoryunov.learnWordsBot.telegram.api.entities.ReplyMarkup
import ru.avgoryunov.learnWordsBot.telegram.api.entities.InlineKeyboard
import ru.avgoryunov.learnWordsBot.telegram.api.entities.SendPhotoResponse
import ru.avgoryunov.learnWordsBot.trainer.LearnWordsTrainer
import ru.avgoryunov.learnWordsBot.trainer.model.Question
import ru.avgoryunov.learnWordsBot.trainer.model.Statistics
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Random
import kotlin.toString

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

    fun checkNextQuestion(chatId: Long, trainer: LearnWordsTrainer, dictionary: DatabaseUserDictionary): Question? {
        val question = trainer.getNextQuestion(chatId, dictionary)

        return if (question != null) question
        else {
            val message = "Все слова в словаре выучены"
            sendMessage(chatId, message, replyMarkup = null)
            null
        }
    }

    // Отправка сообщений

    fun sendMenu(chatId: Long) {
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
        sendMessage(requestBody.chatId, requestBody.text, requestBody.replyMarkup)
    }

    fun sendProgress(chatId: Long, statistics: Statistics?, dictionary: DatabaseUserDictionary) {
        val text = if (statistics != null) {
            val progressBar = "█".repeat(statistics.percentOfLearnedWords / 10) +
                    "▒".repeat(10 - statistics.percentOfLearnedWords / 10)
            "Выучено ${statistics.numberOfLearnedWords} из ${statistics.numberOfTotalWords} слов " +
                    "| ${statistics.percentOfLearnedWords}%\n[$progressBar]"
        } else "Отсутствуют слова в словаре"
        val requestBody = SendMessageRequest(
            chatId = chatId,
            text = text,
        )
        val messageId = sendMessage(requestBody.chatId, requestBody.text, requestBody.replyMarkup)
        val columnName = "message_id_with_statistics"
        dictionary.setMessageId(chatId, messageId, columnName)
    }

    fun sendQuestion(chatId: Long, question: Question, dictionary: DatabaseUserDictionary) {
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
        val messageId = sendMessage(requestBody.chatId, requestBody.text, requestBody.replyMarkup)
        val columnName = "message_id_with_question"
        dictionary.setMessageId(chatId, messageId, columnName)
    }

    fun checkPhotoAndSend(chatId: Long, question: Question, dictionary: DatabaseUserDictionary) {
        // проверка наличия filePath и fileId
        var filePath = dictionary.getFilePath(question.correctAnswer)
        var fileId = dictionary.getFileId(question.correctAnswer)

        if (filePath != null) {
            // отправка фото через fileId либо filePath (при отсутствии первого)
            val photoResponse = sendPhoto(chatId, fileId, File(filePath))
            val responseString = json.decodeFromString<EditMessageResponse>(photoResponse)
            val messageId = responseString.result?.messageId
            val columnName = "message_id_with_photo"
            dictionary.setMessageId(chatId, messageId, columnName)
            // сохранение fileId
            if (fileId == null) {
                val sendPhotoResponse = json.decodeFromString<SendPhotoResponse>(photoResponse)
                val lastPhotoIndex = sendPhotoResponse.result.photo.lastIndex
                fileId = sendPhotoResponse.result.photo.getOrNull(lastPhotoIndex)?.fileId
                dictionary.setFileId(question.correctAnswer, fileId)
            }
        } else {
            filePath = dictionary.getFilePathForEmptyPhoto()
            fileId = dictionary.getFileIdForEmptyPhoto()
            val photoResponse = sendPhoto(chatId, fileId, File(filePath))
            // сохранение messageId
            val responseString = json.decodeFromString<EditMessageResponse>(photoResponse)
            val messageId = responseString.result?.messageId
            val columnName = "message_id_with_photo"
            dictionary.setMessageId(chatId, messageId, columnName)
            // сохранение fileId
            if (fileId == null) {
                val sendPhotoResponse = json.decodeFromString<SendPhotoResponse>(photoResponse)
                val lastPhotoIndex = sendPhotoResponse.result.photo.lastIndex
                fileId = sendPhotoResponse.result.photo.getOrNull(lastPhotoIndex)?.fileId
                dictionary.setFileIdForEmptyPhoto(fileId)
            }
        }
    }

    // Шаблон отправки текстовых сообщений
    fun sendMessage(chatId: Long, text: String, replyMarkup: ReplyMarkup?): Long? {
        val urlSendMessage = "$BOT_URL$botToken/sendMessage"
        val requestBody = SendMessageRequest(chatId, text, replyMarkup)
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
        val responseString = json.decodeFromString<EditMessageResponse>(response?.body().toString())
        val messageId = responseString.result?.messageId
        return messageId
    }

    // Шаблон отправки фото
    fun sendPhoto(chatId: Long, fileId: String?, file: File, hasSpoiler: Boolean = false): String {
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

    // Изменение сообщений

    fun showAnswerStatus(
        chatId: Long,
        question: Question,
        answerIsCorrect: Boolean,
        dictionary: DatabaseUserDictionary,
    ) {
        val columnName = "message_id_with_question"
        val messageId = dictionary.getMessageId(chatId, columnName)

        if (messageId != null) {
            val requestBody = EditMessageRequest(
                chatId = chatId,
                messageId = messageId,
                text = if (answerIsCorrect) "✅ Правильно!" else "❌ Неправильно! " +
                        "${question.correctAnswer.original} - это ${question.correctAnswer.translate}",
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
                ),
            )
            safeEditMessageText(requestBody.chatId, requestBody.messageId, requestBody.text, requestBody.replyMarkup)
        }
        Thread.sleep(3000)
    }

    fun updateProgress(chatId: Long, trainer: LearnWordsTrainer, dictionary: DatabaseUserDictionary) {
        val statistics = trainer.getStatistics(chatId, dictionary)
        val columnName = "message_id_with_statistics"
        val messageId = dictionary.getMessageId(chatId, columnName)

        if (messageId != null) {
            val requestBody = EditMessageRequest(
                chatId = chatId,
                messageId = messageId,
                text = if (statistics != null) {
                    val progressBar = "█".repeat(statistics.percentOfLearnedWords / 10) +
                            "▒".repeat(10 - statistics.percentOfLearnedWords / 10)
                    "Выучено ${statistics.numberOfLearnedWords} из ${statistics.numberOfTotalWords} слов " +
                            "| ${statistics.percentOfLearnedWords}%\n[$progressBar]"
                } else "Отсутствуют слова в словаре",
            )
            safeEditMessageText(requestBody.chatId, requestBody.messageId, requestBody.text, requestBody.replyMarkup)
        }
    }

    fun updateQuestion(chatId: Long, question: Question, dictionary: DatabaseUserDictionary) {
        val columnName = "message_id_with_question"
        val messageId = dictionary.getMessageId(chatId, columnName)

        if (messageId != null) {
            val requestBody = EditMessageRequest(
                chatId = chatId,
                messageId = messageId,
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
                ),
            )
            safeEditMessageText(requestBody.chatId, requestBody.messageId, requestBody.text, requestBody.replyMarkup)
            saveMessageStateForUser(
                requestBody.chatId,
                requestBody.messageId,
                requestBody.text,
                requestBody.replyMarkup
            )
        }
    }

    // Обработка ошибок
    fun safeEditMessageText(chatId: Long, messageId: Long, newText: String, replyMarkup: ReplyMarkup?): Boolean {
        return try {
            val response = editMessageText(chatId, messageId, newText, replyMarkup)
            val jsonResponse = json.decodeFromString<EditMessageResponse>(response)
            jsonResponse.ok
        } catch (e: Exception) {
            when {
                e.message?.contains("MESSAGE_NOT_MODIFIED") == true -> {
                    println("Текст не изменился")
                    true
                }

                e.message?.contains("MESSAGE_EDIT_TIME_EXPIRED") == true -> {
                    println("Время редактирования истекло")
                    false
                }

                else -> {
                    println("Ошибка редактирования: ${e.message}")
                    false
                }
            }
        }
    }

    // Шаблон изменения текстовых сообщений
    fun editMessageText(chatId: Long, messageId: Long, newText: String, replyMarkup: ReplyMarkup?): String {
        val urlEditMessage = "$BOT_URL$botToken/editMessageText"
        val requestBody = EditMessageRequest(chatId, messageId, newText, replyMarkup)
        val requestBodyString = json.encodeToString(requestBody)
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlEditMessage))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
            .build()
        val client: HttpClient = HttpClient.newBuilder().build()
        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    // Обработка ошибок
    fun safeEditMessageMedia(chatId: Long, question: Question, dictionary: DatabaseUserDictionary): Boolean {
        return try {
            // проверка наличия filePath и fileId
            var filePath = dictionary.getFilePath(question.correctAnswer)
            var fileId = dictionary.getFileId(question.correctAnswer)
            var editPhotoResponse = ""
            val columnName = "message_id_with_photo"
            val messageId = dictionary.getMessageId(chatId, columnName)

            if (fileId != null) {
                // обновить фото через fileId
                if (messageId != null) {
                    editPhotoResponse = editMessageMedia(chatId, messageId, fileId)
                    saveMessageStateForUser(chatId, messageId, fileId)
                }

            } else if (filePath != null) {
                if (messageId != null) deleteMessage(chatId, messageId)
                val photoResponse = sendPhoto(chatId, fileId, File(filePath))
                // сохранение messageId
                val responseString = json.decodeFromString<EditMessageResponse>(photoResponse)
                val messageId = responseString.result?.messageId
                val columnName = "message_id_with_photo"
                dictionary.setMessageId(chatId, messageId, columnName)
                // сохранение fileId
                val sendPhotoResponse = json.decodeFromString<SendPhotoResponse>(photoResponse)
                val lastPhotoIndex = sendPhotoResponse.result.photo.lastIndex
                fileId = sendPhotoResponse.result.photo.getOrNull(lastPhotoIndex)?.fileId
                dictionary.setFileId(question.correctAnswer, fileId)
                if (messageId != null) saveMessageStateForUser(chatId, messageId, fileId)
            } else {
                filePath = dictionary.getFilePathForEmptyPhoto()
                fileId = dictionary.getFileIdForEmptyPhoto()
                if (fileId != null) {
                    // обновить фото через fileId
                    val columnName = "message_id_with_photo"
                    val messageId = dictionary.getMessageId(chatId, columnName)
                    if (messageId != null) {
                        editPhotoResponse = editMessageMedia(chatId, messageId, fileId)
                        saveMessageStateForUser(chatId, messageId, fileId)
                    }
                } else {
                    if (messageId != null) deleteMessage(chatId, messageId)
                    val photoResponse = sendPhoto(chatId, fileId, File(filePath))
                    // сохранение messageId
                    val responseString = json.decodeFromString<EditMessageResponse>(photoResponse)
                    val messageId = responseString.result?.messageId
                    val columnName = "message_id_with_photo"
                    dictionary.setMessageId(chatId, messageId, columnName)
                    // сохранение fileId
                    val sendPhotoResponse = json.decodeFromString<SendPhotoResponse>(photoResponse)
                    val lastPhotoIndex = sendPhotoResponse.result.photo.lastIndex
                    fileId = sendPhotoResponse.result.photo.getOrNull(lastPhotoIndex)?.fileId
                    dictionary.setFileId(question.correctAnswer, fileId)
                    if (messageId != null) saveMessageStateForUser(chatId, messageId, fileId)
                }
            }
            val jsonResponse = json.decodeFromString<EditMessageResponse>(editPhotoResponse)
            return jsonResponse.ok
        } catch (e: Exception) {
            when {
                e.message?.contains("MESSAGE_NOT_MODIFIED") == true -> {
                    println("Текст не изменился")
                    true
                }

                e.message?.contains("MESSAGE_EDIT_TIME_EXPIRED") == true -> {
                    println("Время редактирования истекло")
                    false
                }

                else -> {
                    println("Ошибка редактирования: ${e.message}")
                    false
                }
            }
        }
    }

    // Шаблон изменения фото
    fun editMessageMedia(
        chatId: Long,
        messageId: Long,
        fileId: String?,
        hasSpoiler: Boolean = false
    ): String {
        val data: MutableMap<String, Any> = LinkedHashMap()
        data["chat_id"] = chatId.toString()
        data["message_id"] = messageId.toString()
        data["media"] = """
            {
                "type": "photo",
                "media": "$fileId"
            }
        """.trimIndent()
        data["has_spoiler"] = hasSpoiler
        val boundary: String = BigInteger(35, Random()).toString()
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$BOT_URL$botToken/editMessageMedia"))
            .postMultipartFormData(boundary, data)
            .build()
        val client: HttpClient = HttpClient.newBuilder().build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun deleteMessage(chatId: Long, messageId: Long) {
        val urlSendMessage = "$BOT_URL$botToken/deleteMessage"
        val requestBody = DeleteMessage(chatId, messageId)
        val requestBodyString = json.encodeToString(requestBody)
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
            .build()
        try {
            client.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: IOException) {
            if (e.message?.contains("GOAWAY") == true) {
                client.send(request, HttpResponse.BodyHandlers.ofString())
            } else {
                println(e.message)
            }
        }
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
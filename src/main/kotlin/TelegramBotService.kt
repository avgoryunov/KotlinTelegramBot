package org.example

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

class TelegramBotService(
    val botToken: String,
) {
    val client: HttpClient = HttpClient.newBuilder().build()

    fun getUpdates(updateId: Long): String {
        val urlGetUpdates = "$TELEGRAM_ADDRESS$botToken/getUpdates?offset=$updateId"
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun sendMessage(chatId: Long, message: String): String {
        val encoded = URLEncoder.encode(message, StandardCharsets.UTF_8)
        println(encoded)
        val urlSendMessage = "$TELEGRAM_ADDRESS$botToken/sendMessage?chat_id=$chatId&text=$encoded"
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage)).build()
        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun sendMenu(chatId: Long): String {
        val urlSendMessage = "$TELEGRAM_ADDRESS$botToken/sendMessage"

        val menuBody = """
            {
            	"chat_id": $chatId,
            	"text": "Меню",
            	"reply_markup": {
            		"inline_keyboard": [
            			[
            				{
            					"text": "Учить слова",
            					"callback_data": "$LEARN_WORDS_CLICKED"
            				},
            				{
            					"text": "Статистика",
            					"callback_data": "$STATISTICS_CLICKED"
            				},
            				{
            					"text": "Выход",
            					"callback_data": "$EXIT_CLICKED"
            				}
            			]
            		]
            	}
            }
        """.trimIndent()

        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(menuBody))
            .build()
        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun checkNextQuestionAndSend(
        trainer: LearnWordsTrainer,
        telegramBotService: TelegramBotService,
        chatId: Long,
    ) {
        val question = trainer.getNextQuestion()

        if (question == null) telegramBotService.sendMessage(chatId, "Все слова в словаре выучены")
        else telegramBotService.sendQuestion(chatId, question)
    }

    fun sendQuestion(chatId: Long, question: Question): String {
        val urlSendMessage = "$TELEGRAM_ADDRESS$botToken/sendMessage"
        var answerButtons = ""

        question.variants.forEachIndexed { index, word -> answerButtons += """
                				{
                					"text": "${question.variants[index].translate}",
                					"callback_data": "${CALLBACK_DATA_ANSWER_PREFIX + index}"
                				}
            """.trimIndent()

            if (index != question.variants.lastIndex) answerButtons += ",\n"
        }

        val questionBody = """
            {
            	"chat_id": "$chatId",
            	"text": "${question.correctAnswer.original}:",
            	"reply_markup": {
            		"inline_keyboard": [
            			[
            				$answerButtons
            			]
            		]
            	}
            }
        """.trimIndent()

        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(questionBody))
            .build()
        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()

    }
}

const val TELEGRAM_ADDRESS = "https://api.telegram.org/bot"
const val LEARN_WORDS_CLICKED = "learn_words_clicked"
const val STATISTICS_CLICKED = "statistics_clicked"
const val EXIT_CLICKED = "exit_clicked"
const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"
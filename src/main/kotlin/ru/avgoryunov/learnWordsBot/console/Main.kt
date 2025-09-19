package ru.avgoryunov.learnWordsBot.console

import ru.avgoryunov.learnWordsBot.dictionary.DEFAULT_DATABASE_NAME
import ru.avgoryunov.learnWordsBot.dictionary.DatabaseUserDictionary
import ru.avgoryunov.learnWordsBot.trainer.model.Question
import ru.avgoryunov.learnWordsBot.trainer.LearnWordsTrainer
import java.sql.DriverManager
import kotlin.use

fun Question.asConsoleString(): String {
    val variants = this.variants
        .mapIndexed { index: Int, word -> "${index + 1} - ${word.translate}" }
        .joinToString("\n")
    return this.correctAnswer.original + "\n" + variants + "\n0 - выйти в меню"
}

fun main() {

    val trainer = LearnWordsTrainer()
    val chatId = null

    val dictionary = try {
        DatabaseUserDictionary()
    } catch (_: Exception) {
        println("Невозможно загрузить словарь")
        return
    }

    while (true) {
        println("\nМеню:")
        println("1 - Учить слова")
        println("2 - Статистика")
        println("3 - Сбросить прогресс обучения")
        println("0 - Выход")

        val incomingValue = readLine()?.toIntOrNull()

        when (incomingValue) {
            1 -> {
                while (true) {
                    val question = trainer.getNextQuestion(chatId, dictionary)

                    if (question == null) {
                        println("Все слова в словаре выучены")
                        break
                    }

                    println(question.asConsoleString())

                    val userAnswerInput = readLine()?.toIntOrNull()

                    if (userAnswerInput == 0) break

                    if (trainer.checkAnswer(chatId, userAnswerInput?.minus(1), dictionary)) println("Правильно!")
                    else println("Неправильно! ${question.correctAnswer.original} – это ${question.correctAnswer.translate}")
                }
            }

            2 -> {
                val statistics = trainer.getStatistics(chatId, dictionary)
                println("Выучено ${statistics.numberOfLearnedWords} из ${statistics.numberOfTotalWords} слов | ${statistics.percentOfLearnedWords}%")
            }

            3 -> {
                dictionary.resetUserProgress(chatId)
                DriverManager.getConnection("jdbc:sqlite:$DEFAULT_DATABASE_NAME").use { connection ->
                    connection.createStatement().use { statement ->
                        statement.executeUpdate(
                            """
                                DELETE FROM user_answers
                                WHERE user_id is NULL
                                """.trimIndent()
                        )
                    }
                }
                println("Прогресс сброшен")
            }

            0 -> return

            else -> println("Введите число 1, 2, 3 или 0")
        }
    }
}
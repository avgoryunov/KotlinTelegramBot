package ru.avgoryunov.learnWordsBot.console

import ru.avgoryunov.learnWordsBot.dictionary.DatabaseUserDictionary
import ru.avgoryunov.learnWordsBot.trainer.model.Question
import ru.avgoryunov.learnWordsBot.trainer.LearnWordsTrainer

fun Question?.asConsoleString(): String? {
    return if (this?.variants.isNullOrEmpty()) null
    else {
        val variants = this.variants
            .mapIndexed { index: Int, word -> "${index + 1} - ${word.translate.filter { it.isLetterOrDigit() || it.isWhitespace() }}" }
            .joinToString("\n")
        this.correctAnswer.original.filter { it.isLetterOrDigit() || it.isWhitespace() } + "\n" + variants + "\n0 - выйти в меню"
    }
}

fun main() {
    val dictionary = DatabaseUserDictionary()
    val chatId = 1L
    val userName = "user"
    val check = dictionary.checkTheDatabaseStructure()

    if (!check) {
        println("Невозможно загрузить словарь")
        return
    }

    dictionary.addNewUser(userName, chatId)

    val trainer = LearnWordsTrainer()

    while (true) {
        println("\nМеню:")
        println("1 - Учить слова")
        println("2 - Статистика")
        println("3 - Сбросить прогресс обучения")
        println("0 - Выход")

        val incomingValue = readlnOrNull()?.toIntOrNull()

        when (incomingValue) {
            1 -> {
                while (true) {
                    val question = trainer.getNextQuestion(chatId, dictionary)

                    if (question == null) {
                        println("Все слова в словаре выучены")
                        break
                    }

                    println(question.asConsoleString())

                    val userAnswerInput = readlnOrNull()?.toIntOrNull()

                    if (userAnswerInput == 0) break

                    if (trainer.checkAnswer(chatId, question, userAnswerInput?.minus(1), dictionary)) println("Правильно!")
                    else println("Неправильно! ${question.correctAnswer.original} – это ${question.correctAnswer.translate}")
                }
            }

            2 -> {
                val statistics = trainer.getStatistics(chatId, dictionary)
                if (statistics != null) println(
                    "Выучено ${statistics.numberOfLearnedWords} из " +
                            "${statistics.numberOfTotalWords} слов | ${statistics.percentOfLearnedWords}%"
                )
                else println("Отсутствуют слова в словаре")
            }

            3 -> {
                dictionary.resetUserProgress(chatId)
                println("Прогресс сброшен")
            }

            0 -> return

            else -> println("Введите число 1, 2, 3 или 0")
        }
    }
}
package org.example

fun Question.asConsoleString(): String {
    val variants = this.variants
        .mapIndexed { index: Int, word -> "${index + 1} - ${word.translate}" }
        .joinToString("\n")
    return this.correctAnswer.original + "\n" + variants + "\n0 - выйти в меню"
}

fun main(trainer: LearnWordsTrainer) {

    while (true) {
        println("\nМеню:")
        println("1 - Учить слова")
        println("2 - Статистика")
        println("0 - Выход")

        val incomingValue = readLine()?.toIntOrNull()

        when (incomingValue) {
            1 -> {
                while (true) {
                    val question = trainer.getNextQuestion()

                    if (question == null) {
                        println("Все слова в словаре выучены")
                        break
                    }

                    println(question.asConsoleString())

                    val userAnswerInput = readLine()?.toIntOrNull()

                    if (userAnswerInput == 0) break

                    if (trainer.checkAnswer(userAnswerInput?.minus(1))) println("Правильно!")
                    else println("Неправильно! ${question.correctAnswer.original} – это ${question.correctAnswer.translate}")
                }
            }

            2 -> {
                val statistics = trainer.getStatistics()
                println("Выучено ${statistics.learned} из ${statistics.total} слов | ${statistics.percentLearned}%")
            }

            0 -> return

            else -> println("Введите число 1, 2 или 0")
        }
    }
}
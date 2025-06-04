package org.example

import java.io.File

data class Word(
    val original: String,
    val translate: String,
    var correctAnswersCount: Int = 0,
)

fun main() {
    val dictionary = loadDictionary()

    while (true) {
        println("\nМеню:")
        println("1 - Учить слова")
        println("2 - Статистика")
        println("0 - Выход")

        val incomingValue = readLine()

        when (incomingValue) {
            "1" -> {
                println("Выбран пункт \"Учить слова\"")

                while (true) {
                    val notLearnedList = getNotLearnedWords(dictionary)

                    if (notLearnedList.isEmpty()) {
                        println("Все слова в словаре выучены")
                        break
                    }

                    val questionWords: List<Word> = notLearnedList.shuffled().take(NUMBER_QUESTION_WORDS)
                    val correctAnswer = questionWords.random()

                    println("\n${correctAnswer.original}:")

                    for (i in questionWords.indices) {
                        println("${i + 1} - ${questionWords[i].translate}")
                    }

                    println("----------")
                    println("0 - Меню")

                    val userAnswerInput: String? = readLine()

                    when (userAnswerInput) {
                        "0" -> break
                    }

                    val correctAnswerId = questionWords.indexOf(correctAnswer)

                    if (userAnswerInput == (correctAnswerId + 1).toString()) {
                        println("Правильно!")
                        questionWords[correctAnswerId].correctAnswersCount += 1
                        saveDictionary(dictionary)
                    } else println("Неправильно! ${correctAnswer.original} – это ${correctAnswer.translate}")
                }
            }

            "2" -> {
                println("Выбран пункт \"Статистика\"")
                val totalCount = dictionary.count()
                val learnedCount = getLearnedWords(dictionary).count()
                val percentLearnedCount = learnedCount * FULL_PERCENT / totalCount
                println("Выучено $learnedCount из $totalCount слов | $percentLearnedCount%")
            }

            "0" -> return

            else -> println("Введите число 1, 2 или 0")
        }
    }
}

fun loadDictionary(): List<Word> {
    val wordsFile = File("words.txt")
    wordsFile.createNewFile()

    val dictionary: MutableList<Word> = mutableListOf()
    val lines: List<String> = wordsFile.readLines()

    for (line in lines) {
        val line = line.split("|")
        val word = Word(original = line[0], translate = line[1], correctAnswersCount = line[2].toIntOrNull() ?: 0)
        dictionary.add(word)
    }
    return dictionary
}

fun getLearnedWords(dictionary: List<Word>): List<Word> {
    return dictionary.filter { it.correctAnswersCount >= MIN_CORRECT_ANSWERS_COUNT }
}

fun getNotLearnedWords(dictionary: List<Word>): List<Word> {
    return dictionary.filter { it.correctAnswersCount < MIN_CORRECT_ANSWERS_COUNT }
}

fun saveDictionary(dictionary: List<Word>) {
    File("words.txt").printWriter().use { out ->
        dictionary.forEach { word ->
            out.println("${word.original}|${word.translate}|${word.correctAnswersCount}")
        }
    }
}

const val FULL_PERCENT = 100
const val MIN_CORRECT_ANSWERS_COUNT = 3
const val NUMBER_QUESTION_WORDS = 4
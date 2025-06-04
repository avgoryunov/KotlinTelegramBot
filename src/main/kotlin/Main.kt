package org.example

import java.io.File

data class Word(
    val original: String,
    val translate: String,
    val correctAnswersCount: Int = 0,
)

fun main() {
    val dictionary = loadDictionary()

    while (true) {
        println()
        println("Меню:")
        println("1 - Учить слова")
        println("2 - Статистика")
        println("0 - Выход")

        val incomingValue = readLine()

        when (incomingValue) {
            "1" -> {
                println("Выбран пункт \"Учить слова\"")
                val notLearnedList = getNotLearnedWords(dictionary)

                while (notLearnedList != emptyList<Word>()) {
                    val questionWords: List<Word> = notLearnedList.shuffled().take(4)
                    val correctAnswer = questionWords.random()

                    println()
                    println("${correctAnswer.original}:")
                    for (i in questionWords.indices) {
                        println("${i + 1} - ${questionWords[i].translate}")
                    }

                    val userAnswerInput = readLine()
                }
                println("Все слова в словаре выучены")
                continue
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

const val FULL_PERCENT = 100
const val MIN_CORRECT_ANSWERS_COUNT = 3
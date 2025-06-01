package org.example

import java.io.File

data class Word(
    val original: String,
    val translate: String,
    val correctAnswersCount: Int = 0,
)

fun main() {
    val dictionary = loadDictionary()

    for (word in dictionary) {
        println(word)
    }

    while (true) {
        println("\nМеню:")
        println("1 - Учить слова")
        println("2 - Статистика")
        println("0 - Выход")

        val incomingValue = readLine()

        when (incomingValue) {
            "1" -> println("Выбран пункт \"Учить слова\"")
            "2" -> {
                println("Выбран пункт \"Статистика\"")
                val totalCount = dictionary.count()
                val learnedCount = dictionary.filter(3).count()
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

    wordsFile.writeText("hello|привет|0")
    wordsFile.appendText("\n")
    wordsFile.appendText("dog|собака|0")
    wordsFile.appendText("\n")
    wordsFile.appendText("cat|кошка|3")

    val dictionary: MutableList<Word> = mutableListOf()
    val lines: List<String> = wordsFile.readLines()

    for (line in lines) {
        val line = line.split("|")
        val word = Word(original = line[0], translate = line[1], correctAnswersCount = line[2].toIntOrNull() ?: 0)
        dictionary.add(word)
    }
    return dictionary
}

fun List<Word>.filter(minCorrectAnswersCount: Int): List<Word> {
    return this.filter { dictionary -> dictionary.correctAnswersCount >= minCorrectAnswersCount }
}

const val FULL_PERCENT = 100
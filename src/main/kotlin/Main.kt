package org.example

import java.io.File

data class Word(
    val original: String,
    val translate: String,
    val correctAnswersCount: Int = 0,
)

fun main() {
    val wordsFile = File("words.txt")
    wordsFile.createNewFile()

    wordsFile.writeText("hello|привет|0")
    wordsFile.appendText("\n")
    wordsFile.appendText("dog|собака|0")
    wordsFile.appendText("\n")
    wordsFile.appendText("cat|кошка|0")

    val dictionary: MutableList<Word> = mutableListOf()
    val lines: List<String> = wordsFile.readLines()

    for (line in lines) {
        val line = line.split("|")
        val word = Word(original = line[0], translate = line[1], correctAnswersCount = line[2].toIntOrNull() ?: 0)
        dictionary.add(word)
    }

    for (word in dictionary) {
        println(word)
    }
}
package org.example

import java.io.File

fun main() {

    val wordsFile = File("words.txt")
    wordsFile.createNewFile()

    wordsFile.writeText("hello привет\n")
    wordsFile.appendText("dog собака\n")
    wordsFile.appendText("cat кошка")

    wordsFile.readLines().forEach { it -> println(it) }
}
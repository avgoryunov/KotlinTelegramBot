package org.example

import java.io.File

data class Statistics(
    val learned: Int,
    val total: Int,
    val percentLearned: Int,
)

data class Question(
    val variants: List<Word>,
    val correctAnswer: Word,
)

class LearnWordsTrainer {
    private var question: Question? = null
    private val dictionary = loadDictionary()

    fun getStatistics(): Statistics {
        val learned = dictionary.count { it.correctAnswersCount >= MIN_CORRECT_ANSWERS_COUNT }
        val total = dictionary.count()
        val percentLearned = learned * FULL_PERCENT / total
        return Statistics(learned, total, percentLearned)
    }

    fun getNextQuestion(): Question? {
        val notLearnedList = dictionary.filter { it.correctAnswersCount < MIN_CORRECT_ANSWERS_COUNT }
        if (notLearnedList.isEmpty()) return null
        val learnedList = dictionary.filter { it.correctAnswersCount >= MIN_CORRECT_ANSWERS_COUNT }
        val questionWords =
            if (notLearnedList.size > NUMBER_QUESTION_WORDS) notLearnedList.shuffled().take(NUMBER_QUESTION_WORDS)
            else {
                val addQuestionWords = learnedList.shuffled().take(NUMBER_QUESTION_WORDS - notLearnedList.size)
                (notLearnedList + addQuestionWords).shuffled()
            }
        val correctAnswer = questionWords.random()
        question = Question(
            variants = questionWords,
            correctAnswer = correctAnswer,
        )
        return question
    }

    fun checkAnswer(userAnswerIndex: Int?): Boolean {
        return question?.let {
            val correctAnswerId = it.variants.indexOf(it.correctAnswer)
            if (correctAnswerId == userAnswerIndex) {
                it.correctAnswer.correctAnswersCount++
                saveDictionary(dictionary)
                true
            } else {
                false
            }
        } ?: false
    }

    private fun loadDictionary(): List<Word> {
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

    private fun saveDictionary(dictionary: List<Word>) {
        File("words.txt").printWriter().use { out ->
            dictionary.forEach { word ->
                out.println("${word.original}|${word.translate}|${word.correctAnswersCount}")
            }
        }
    }
}

const val FULL_PERCENT = 100
const val NUMBER_QUESTION_WORDS = 4
const val MIN_CORRECT_ANSWERS_COUNT = 3
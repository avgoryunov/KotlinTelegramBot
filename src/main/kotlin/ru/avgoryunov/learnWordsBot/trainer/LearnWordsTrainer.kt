package ru.avgoryunov.learnWordsBot.trainer

import ru.avgoryunov.learnWordsBot.dictionary.DatabaseUserDictionary
import ru.avgoryunov.learnWordsBot.trainer.model.Statistics
import ru.avgoryunov.learnWordsBot.trainer.model.Question

class LearnWordsTrainer(
    private var countVariants: Int = 4,
) {
    var question: Question? = null

    fun getStatistics(chatId: Long?, dictionary: DatabaseUserDictionary): Statistics? {
        return try {
            val numberOfLearnedWords = dictionary.getNumberOfLearnedWords(chatId)
            val numberOfTotalWords = dictionary.getSize()
            val percentOfLearnedWords = numberOfLearnedWords * FULL_PERCENT / numberOfTotalWords
            return Statistics(numberOfLearnedWords, numberOfTotalWords, percentOfLearnedWords)
        } catch (e: ArithmeticException) {
            e.message
            null
        }

    }

    fun getNextQuestion(chatId: Long?, dictionary: DatabaseUserDictionary): Question? {
        if (countVariants > MAX_COUNT_VARIANTS) countVariants = MAX_COUNT_VARIANTS
        val learnedWords = dictionary.getLearnedWords(chatId)
        val unlearnedWords = dictionary.getUnlearnedWords(chatId)
        if (unlearnedWords.isEmpty() || countVariants == 0) return null
        val variants =
            if (unlearnedWords.size < countVariants) {
                unlearnedWords.shuffled().take(countVariants) +
                        learnedWords.shuffled().take(countVariants - unlearnedWords.size)
            } else (unlearnedWords.shuffled().take(countVariants)).shuffled()
        val correctAnswer = variants.random()
        question = Question(variants, correctAnswer)
        return question
    }

    fun checkAnswer(chatId: Long?, question: Question?, userAnswerIndex: Int?, dictionary: DatabaseUserDictionary): Boolean {
        return question?.let {
            val correctAnswerId = it.variants.indexOf(it.correctAnswer)
            if (correctAnswerId == userAnswerIndex) {
                val original = it.correctAnswer.original
                val correctAnswersCount = (it.correctAnswer.correctAnswersCount) + 1
                dictionary.setCorrectAnswersCount(chatId, original, correctAnswersCount)
                true
            } else false
        } ?: false
    }
}

const val FULL_PERCENT = 100
const val MAX_COUNT_VARIANTS = 10
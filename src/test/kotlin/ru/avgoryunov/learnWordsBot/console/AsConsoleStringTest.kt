package ru.avgoryunov.learnWordsBot.console

import org.junit.jupiter.api.assertDoesNotThrow
import ru.avgoryunov.learnWordsBot.dictionary.DatabaseUserDictionary
import ru.avgoryunov.learnWordsBot.trainer.LearnWordsTrainer
import ru.avgoryunov.learnWordsBot.trainer.model.Question
import ru.avgoryunov.learnWordsBot.trainer.model.Word
import kotlin.test.Test
import kotlin.test.assertEquals

class AsConsoleStringTest {

    @Test
    fun `a regular case with 4 variants`() {
        val variants = listOf(
            Word("one", "один", 1),
            Word("two", "два", 1),
            Word("three", "три", 1),
            Word("four", "четыре", 1),
        )
        val correctAnswer = variants[2]

        val expected =
            """
                three
                1 - один
                2 - два
                3 - три
                4 - четыре
                0 - выйти в меню
                """.trimIndent()

        val question = Question(variants, correctAnswer)
        val actual = question.asConsoleString()

        assertEquals(expected, actual)
    }

    @Test
    fun `the order of variants has been changed`() {
        val variants = listOf(
            Word("three", "три", 1),
            Word("one", "один", 1),
            Word("four", "четыре", 1),
            Word("two", "два", 1),
        )
        val correctAnswer = variants[1]
        val expected =
            """
                one
                1 - три
                2 - один
                3 - четыре
                4 - два
                0 - выйти в меню
                """.trimIndent()
        val question = Question(variants, correctAnswer)
        val actual = question.asConsoleString()

        assertEquals(expected, actual)
    }

    @Test
    fun `an empty list of variants`() {
        val dictionary = DatabaseUserDictionary()
        val trainer = LearnWordsTrainer(countVariants = 0)
        val question = trainer.getNextQuestion(1, dictionary)
        val expected = null
        val actual = question.asConsoleString()

        assertEquals(expected, actual)
    }

    @Test
    fun `a list of 10 variants`() {
        val dictionary = DatabaseUserDictionary()
        val trainer = LearnWordsTrainer(countVariants = 10)
        val question = trainer.getNextQuestion(1, dictionary)

        assertDoesNotThrow {question.asConsoleString()}

        val expected = 10
        val actual = question?.variants?.size

        assertEquals(expected, actual)
    }

    @Test
    fun `a list of 200 variants`() {
        val dictionary = DatabaseUserDictionary()
        val trainer = LearnWordsTrainer(countVariants = 200)
        val question = trainer.getNextQuestion(1, dictionary)

        assertDoesNotThrow {question.asConsoleString()}

        val expected = 10
        val actual = question?.variants?.size

        assertEquals(expected, actual)
    }

    @Test
    fun `a test with special characters in words`() {
        val variants = listOf(
            Word("one.)", "один/", 1),
            Word("two!", "два.", 1),
            Word("three?", "три!", 1),
            Word("four/", "четыре)", 1),
        )
        val correctAnswer = variants[3]

        val expected =
            """
                four
                1 - один
                2 - два
                3 - три
                4 - четыре
                0 - выйти в меню
                """.trimIndent()

        val question = Question(variants, correctAnswer)
        val actual = question.asConsoleString()

        assertEquals(expected, actual)
    }

    @Test
    fun `words consisting of spaces`() {
        val variants = listOf(
            Word("thank you", "спасибо", 1),
            Word("excuse me", "извините", 1),
            Word("good luck", "удачи", 1),
            Word("hurry up", "поторопись", 1),
        )
        val correctAnswer = variants[3]

        val expected =
            """
                hurry up
                1 - спасибо
                2 - извините
                3 - удачи
                4 - поторопись
                0 - выйти в меню
                """.trimIndent()

        val question = Question(variants, correctAnswer)
        val actual = question.asConsoleString()

        assertEquals(expected, actual)
    }
}
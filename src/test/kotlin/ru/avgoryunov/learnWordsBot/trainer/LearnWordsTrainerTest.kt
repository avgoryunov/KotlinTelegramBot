package ru.avgoryunov.learnWordsBot.trainer

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import ru.avgoryunov.learnWordsBot.dictionary.DatabaseUserDictionary
import ru.avgoryunov.learnWordsBot.trainer.model.Question
import ru.avgoryunov.learnWordsBot.trainer.model.Statistics
import ru.avgoryunov.learnWordsBot.trainer.model.Word
import java.sql.DriverManager
import java.sql.SQLException
import kotlin.use

class LearnWordsTrainerTest {

    @Test
    fun `test statistics with 4 words of 7`() {
        val chatId = 1L
        val database = "src/test/kotlin/ru/avgoryunov/learnWordsBot/trainer/4_words_of_7.db"
        val dictionary = DatabaseUserDictionary(database)
        val trainer = LearnWordsTrainer()

        kotlin.test.assertEquals(
            Statistics(4, 7, 57),
            trainer.getStatistics(chatId, dictionary)
        )
    }

    @Test
    fun `test statistics with corrupted file`() {
        val chatId = 1L
        val database = "src/test/kotlin/ru/avgoryunov/learnWordsBot/trainer/corrupted_file.db"
        val dictionary = DatabaseUserDictionary(database)
        val trainer = LearnWordsTrainer()

        kotlin.test.assertEquals(
            null,
            trainer.getStatistics(chatId, dictionary)
        )
    }

    @Test
    fun `test getNextQuestion() with 5 unlearned words`() {
        val chatId = 1L
        val database = "src/test/kotlin/ru/avgoryunov/learnWordsBot/trainer/5_unlearned_words.db"
        val dictionary = DatabaseUserDictionary(database)
        val trainer = LearnWordsTrainer()

        assertDoesNotThrow { trainer.getNextQuestion(chatId, dictionary) }

        val expected = 4
        val actual = trainer.question?.variants?.size

        kotlin.test.assertEquals(expected, actual)
    }

    @Test
    fun `test getNextQuestion() with 1 unlearned word`() {
        val chatId = 1L
        val database = "src/test/kotlin/ru/avgoryunov/learnWordsBot/trainer/1_unlearned_word.db"
        val dictionary = DatabaseUserDictionary(database)
        val trainer = LearnWordsTrainer()

        assertDoesNotThrow { trainer.getNextQuestion(chatId, dictionary) }

        val expected = 4
        val actual = trainer.question?.variants?.size
        println(actual)

        kotlin.test.assertEquals(expected, actual)
    }

    @Test
    fun `test getNextQuestion() with all words learned`() {
        val chatId = 1L
        val database = "src/test/kotlin/ru/avgoryunov/learnWordsBot/trainer/all_words_learned.db"
        val dictionary = DatabaseUserDictionary(database)
        val trainer = LearnWordsTrainer()

        assertDoesNotThrow { trainer.getNextQuestion(chatId, dictionary) }

        val expected = null
        val actual = trainer.question?.variants?.size
        println(actual)

        kotlin.test.assertEquals(expected, actual)
    }

    @Test
    fun `test checkAnswer() with true`() {
        val database = "src/test/kotlin/ru/avgoryunov/learnWordsBot/trainer/check_answer_with_true.db"

        try {
            DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeUpdate(
                        """
                        DROP TABLE IF EXISTS words;
                        DROP TABLE IF EXISTS users;
                        DROP TABLE IF EXISTS user_answers;

                        PRAGMA foreign_keys=on;

                        CREATE TABLE IF NOT EXISTS 'user_answers' (
                        'user_id' integer,
                        'word_id' integer,
                        'correct_answer_count' integer,
                        'updated_at' timestamp,
                        FOREIGN KEY ('user_id') REFERENCES 'users' ('id'),
                        FOREIGN KEY ('word_id') REFERENCES 'words' ('id'),
                        UNIQUE ('user_id', 'word_id')
                        );

                        CREATE TABLE IF NOT EXISTS 'users' (
                        'id' integer,
                        'username' varchar,
                        'created_at' timestamp,
                        'chat_id' integer,
                        UNIQUE ('chat_id'),
                        PRIMARY KEY ('id' AUTOINCREMENT)
                        );

                        CREATE TABLE IF NOT EXISTS 'words' (
                        'id' integer,
                        'text' varchar,
                        'translate' varchar,
                        UNIQUE ('text'),
                        PRIMARY KEY ('id' AUTOINCREMENT)
                        );
                        
                        INSERT INTO words ('text', 'translate') VALUES ('define', 'определять');
                        INSERT INTO words ('text', 'translate') VALUES ('solve', 'решать');
                        INSERT INTO words ('text', 'translate') VALUES ('serve', 'служить');
                        INSERT INTO words ('text', 'translate') VALUES ('empty', 'пустой');
                        INSERT INTO users (username, created_at, chat_id) VALUES ('user', CURRENT_TIMESTAMP, 1);
                        INSERT INTO user_answers (user_id, word_id, correct_answer_count, updated_at) VALUES (1, 3, 1, CURRENT_TIMESTAMP);
                        """.trimIndent()
                    )
                }
            }
        } catch (e: SQLException) {
            println(e.message)
        }

        val chatId = 1L
        val expectedBefore = true
        val actualBefore =
            DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery(
                        """
                            SELECT EXISTS (SELECT * FROM user_answers 
                            WHERE (SELECT id FROM users WHERE chat_id = 1) 
                            AND (SELECT id FROM words WHERE text = 'serve') 
                            AND correct_answer_count = 1)
                            """.trimIndent()
                    ).use { resultSet ->
                        resultSet.getBoolean(1)
                    }
                }
            }

        kotlin.test.assertEquals(expectedBefore, actualBefore)

        val dictionary = DatabaseUserDictionary(database)
        val trainer = LearnWordsTrainer()

        val variants = listOf(
            Word("define", "определять", 0),
            Word("solve", "решать", 0),
            Word("serve", "служить", 1),
            Word("empty", "пустой", 0),
        )
        val correctAnswer = Word("serve", "служить", 1)
        val question = Question(variants, correctAnswer)
        val userAnswerIndex = 2


        val expected = true
        val actual = trainer.checkAnswer(chatId, question, userAnswerIndex, dictionary)

        kotlin.test.assertEquals(expected, actual)

        val expectedAfter = true
        val actualAfter =
            DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery(
                        """
                            SELECT EXISTS (SELECT * FROM user_answers 
                            WHERE (SELECT id FROM users WHERE chat_id = 1) 
                            AND (SELECT id FROM words WHERE text = 'serve') 
                            AND correct_answer_count = 2)
                            """.trimIndent()
                    ).use { resultSet ->
                        resultSet.getBoolean(1)
                    }
                }
            }

        kotlin.test.assertEquals(expectedAfter, actualAfter)
    }

    @Test
    fun `test checkAnswer() with false`() {
        val trainer = LearnWordsTrainer()
        val chatId = 1L
        val variants = listOf(
            Word("define", "определять", 3),
            Word("solve", "решать", 3),
            Word("serve", "служить", 3),
            Word("empty", "пустой", 3),
        )
        val correctAnswer = Word("serve", "служить", 3)
        val question = Question(variants, correctAnswer)
        val userAnswerIndex = 0
        val database = "src/test/kotlin/ru/avgoryunov/learnWordsBot/trainer/all_words_learned.db"
        val dictionary = DatabaseUserDictionary(database)

        val expected = false
        val actual = trainer.checkAnswer(chatId, question, userAnswerIndex, dictionary)

        kotlin.test.assertEquals(expected, actual)
    }
}
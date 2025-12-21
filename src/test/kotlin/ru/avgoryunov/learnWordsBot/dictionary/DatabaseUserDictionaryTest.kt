package ru.avgoryunov.learnWordsBot.dictionary

import org.junit.jupiter.api.Test
import ru.avgoryunov.learnWordsBot.trainer.model.Word
import java.sql.DriverManager
import java.sql.SQLException
import kotlin.test.assertFailsWith
import kotlin.use

class DatabaseUserDictionaryTest {
    @Test
    fun `test get number learned words`() {
        val chatId = 1L
        val database = "src/test/kotlin/ru/avgoryunov/learnWordsBot/dictionary/data_from_the_database.db"
        val dictionary = DatabaseUserDictionary(database)
        val expected = 3
        val actual = dictionary.getNumberOfLearnedWords(chatId)

        kotlin.test.assertEquals(expected, actual)
    }

    @Test
    fun `test get number of words`() {
        val database = "src/test/kotlin/ru/avgoryunov/learnWordsBot/dictionary/data_from_the_database.db"
        val dictionary = DatabaseUserDictionary(database)
        val expected = 7
        val actual = dictionary.getSize()

        kotlin.test.assertEquals(expected, actual)
    }

    @Test
    fun `test get learned words list`() {
        val chatId = 1L
        val database = "src/test/kotlin/ru/avgoryunov/learnWordsBot/dictionary/data_from_the_database.db"
        val dictionary = DatabaseUserDictionary(database)
        val expected = listOf(
            Word("full", "полный", 3),
            Word("narrow", "узкий", 3),
            Word("wide", "широкий", 3),
        )
        val actual = dictionary.getLearnedWords(chatId)

        kotlin.test.assertEquals(expected, actual)
    }

    @Test
    fun `test get unlearned words list`() {
        val chatId = 1L
        val database = "src/test/kotlin/ru/avgoryunov/learnWordsBot/dictionary/data_from_the_database.db"
        val dictionary = DatabaseUserDictionary(database)
        val expected = listOf(
            Word("define", "определять", 0),
            Word("solve", "решать", 0),
            Word("serve", "служить", 1),
            Word("empty", "пустой", 2),
        )
        val actual = dictionary.getUnlearnedWords(chatId)

        kotlin.test.assertEquals(expected, actual)
    }

    @Test
    fun `test set correct answers count`() {
        val database = "src/test/kotlin/ru/avgoryunov/learnWordsBot/dictionary/correct_answers_count.db"

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
                        INSERT INTO users (username, created_at, chat_id) VALUES ('user', CURRENT_TIMESTAMP, 1);
                        """.trimIndent()
                    )
                }
            }
        } catch (e: SQLException) {
            println(e.message)
        }

        val chatId = 1L
        val expectedBefore = false
        val actualBefore =
            DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
                val sql = "SELECT EXISTS (SELECT * FROM user_answers WHERE (SELECT id FROM users WHERE chat_id = ?))"
                connection.prepareStatement(sql).use { statement ->
                    statement.setLong(1, chatId)
                    statement.executeQuery().use { resultSet ->
                        resultSet.getBoolean(1)
                    }
                }
            }

        kotlin.test.assertEquals(expectedBefore, actualBefore)

        val dictionary = DatabaseUserDictionary(database)

        val original = "define"
        val correctAnswersCount = 1

        dictionary.setCorrectAnswersCount(chatId, original, correctAnswersCount)

        val expectedAfter = true
        val actualAfter =
            DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
                val sql = "SELECT EXISTS (SELECT * FROM user_answers WHERE (SELECT id FROM users WHERE chat_id = ?))"
                connection.prepareStatement(sql).use { statement ->
                    statement.setLong(1, chatId)
                    statement.executeQuery().use { resultSet ->
                        resultSet.getBoolean(1)
                    }
                }
            }

        kotlin.test.assertEquals(expectedAfter, actualAfter)
    }

    @Test
    fun `test resetUserProgress() with 2 words in dictionary`() {
        val database = "src/test/kotlin/ru/avgoryunov/learnWordsBot/dictionary/user_progress.db"

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
                        INSERT INTO users (username, created_at, chat_id) VALUES ('user', CURRENT_TIMESTAMP, 1);
                        INSERT INTO user_answers values (1, 1, 1, CURRENT_TIMESTAMP);
                        INSERT INTO user_answers values (1, 2, 1, CURRENT_TIMESTAMP);
                        """.trimIndent()
                    )
                }
            }
        } catch (e: SQLException) {
            println(e.message)
        }

        val chatId = 1L
        val expectedBefore = 2
        val actualBefore = DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
            val sql = "SELECT COUNT (*) FROM user_answers WHERE user_id = (SELECT (id) FROM users WHERE chat_id = ?)"
            connection.prepareStatement(sql).use { statement ->
                statement.setLong(1, chatId)
                statement.executeQuery().use { resultSet ->
                    resultSet.getInt(1)
                }
            }
        }

        kotlin.test.assertEquals(expectedBefore, actualBefore)

        val dictionary = DatabaseUserDictionary(database)

        dictionary.resetUserProgress(chatId)

        val expectedAfter = 0
        val actualAfter = DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
            val sql = "SELECT COUNT (*) FROM user_answers WHERE user_id = (SELECT (id) FROM users WHERE chat_id = ?)"
            connection.prepareStatement(sql).use { statement ->
                statement.setLong(1, chatId)
                statement.executeQuery().use { resultSet ->
                    resultSet.getInt(1)
                }
            }
        }

        kotlin.test.assertEquals(expectedAfter, actualAfter)
    }

    @Test
    fun `test addNewUser()`() {
        val database = "src/test/kotlin/ru/avgoryunov/learnWordsBot/dictionary/new_user.db"

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
                        """.trimIndent()
                    )
                }
            }
        } catch (e: SQLException) {
            println(e.message)
        }

        val chatId = 1L
        val expectedBefore = 0
        val actualBefore = DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
            val sql = "SELECT EXISTS (SELECT * FROM users WHERE chat_id = ?)"
            connection.prepareStatement(sql).use { statement ->
                statement.setLong(1, chatId)
                statement.executeQuery().use { resultSet ->
                    resultSet.getInt(1)
                }
            }
        }

        kotlin.test.assertEquals(expectedBefore, actualBefore)

        val dictionary = DatabaseUserDictionary(database)
        val userName = "user"

        dictionary.addNewUser(chatId, userName)

        val expectedAfter = 1
        val actualAfter = DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
            val sql = "SELECT EXISTS (SELECT * FROM users WHERE chat_id = ?)"
            connection.prepareStatement(sql).use { statement ->
                statement.setLong(1, chatId)
                statement.executeQuery().use { resultSet ->
                    resultSet.getInt(1)
                }
            }
        }

        kotlin.test.assertEquals(expectedAfter, actualAfter)
    }

    @Test
    fun `test validateInputData()`() {
        val inputData = "'; DELETE FROM words WHERE 1=1; --"

        try {
            assertFailsWith<IllegalArgumentException> { validateInputData(inputData) }
        } catch (e: IllegalArgumentException) {
            e.message
        }
    }

    @Test
    fun `test do sql-injection in setFileId()`() {
        val database = "src/test/kotlin/ru/avgoryunov/learnWordsBot/dictionary/set_file_id_sql_injection.db"

        try {
            DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeUpdate(
                        """
                        DROP TABLE IF EXISTS words;
                        
                        PRAGMA foreign_keys=on;
                        
                        CREATE TABLE IF NOT EXISTS 'words' (
                        'id' integer,
                        'text' varchar,
                        'translate' varchar,
                        'photo_file_id' varchar,
                        UNIQUE ('text'),
                        PRIMARY KEY ('id' AUTOINCREMENT)
                        );
                        
                        INSERT INTO words ('text', 'translate') VALUES ('one', 'один');
                        INSERT INTO words ('text', 'translate') VALUES ('two', 'два');
                        INSERT INTO words ('text', 'translate') VALUES ('three', 'три');
                        """.trimIndent()
                    )
                }
            }
        } catch (e: SQLException) {
            println("Ошибка: ${e.message}")
        }

        val expectedBefore = 3
        val actualBefore = DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
            val sql = "SELECT COUNT (*) FROM words"
            connection.createStatement().use { statement ->
                statement.executeQuery(sql).use { resultSet ->
                    resultSet.getInt(1)
                }
            }
        }

        kotlin.test.assertEquals(expectedBefore, actualBefore)

        val dictionary = DatabaseUserDictionary(database)
        val text = Word("one", "один")
        val fileId = "'; DELETE FROM words WHERE 1=1; --"

        dictionary.setFileId(text, fileId)

        val expectedAfter = 3
        val actualAfter = DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
            val sql = "SELECT COUNT (*) FROM words"
            connection.createStatement().use { statement ->
                statement.executeQuery(sql).use { resultSet ->
                    resultSet.getInt(1)
                }
            }
        }

        kotlin.test.assertEquals(expectedAfter, actualAfter)
    }

    @Test
    fun `test do sql-injection in getFileId()`() {
        val database = "src/test/kotlin/ru/avgoryunov/learnWordsBot/dictionary/get_file_id_sql_injection.db"

        try {
            DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeUpdate(
                        """
                        DROP TABLE IF EXISTS words;
                        
                        PRAGMA foreign_keys=on;
                        
                        CREATE TABLE IF NOT EXISTS 'words' (
                        'id' integer,
                        'text' varchar,
                        'translate' varchar,
                        'photo_file_id' varchar,
                        UNIQUE ('text'),
                        PRIMARY KEY ('id' AUTOINCREMENT)
                        );
                        
                        INSERT INTO words ('text', 'translate', 'photo_file_id') VALUES ('one', 'один', 'q1');
                        INSERT INTO words ('text', 'translate', 'photo_file_id') VALUES ('two', 'два', 'q2');
                        INSERT INTO words ('text', 'translate', 'photo_file_id') VALUES ('three', 'три', 'q3');
                        """.trimIndent()
                    )
                }
            }
        } catch (e: SQLException) {
            println("Ошибка: ${e.message}")
        }

        val dictionary = DatabaseUserDictionary(database)
        var text = Word("one", "один")
        var fileId = dictionary.getFileId(text)
        val expectedBefore = "q1"
        val actualBefore = fileId

        kotlin.test.assertEquals(expectedBefore, actualBefore)

        text = Word("' OR '1'='1'", "один")
        fileId = dictionary.getFileId(text)
        val expectedAfter = null
        val actualAfter = fileId

        kotlin.test.assertEquals(expectedAfter, actualAfter)
    }

    @Test
    fun `test do sql-injection in getFilePath()`() {
        val database = "src/test/kotlin/ru/avgoryunov/learnWordsBot/dictionary/get_file_path_sql_injection.db"

        try {
            DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeUpdate(
                        """
                        DROP TABLE IF EXISTS words;
                        DROP TABLE IF EXISTS users;
                        
                        PRAGMA foreign_keys=on;
                        
                        CREATE TABLE IF NOT EXISTS 'words' (
                        'id' integer,
                        'text' varchar,
                        'translate' varchar,
                        'photo_file_path' varchar,
                        UNIQUE ('text'),
                        PRIMARY KEY ('id' AUTOINCREMENT)
                        );
                        
                        CREATE TABLE IF NOT EXISTS 'users' (
                        'id' integer,
                        'username' varchar,
                        'created_at' timestamp,
                        'chat_id' integer,
                        UNIQUE ('chat_id'),
                        PRIMARY KEY ('id' AUTOINCREMENT)
                        );
                        
                        INSERT INTO words ('text', 'translate', 'photo_file_path') VALUES ('one', 'один', 'q1');
                        INSERT INTO words ('text', 'translate', 'photo_file_path') VALUES ('two', 'два', 'q2');
                        INSERT INTO words ('text', 'translate', 'photo_file_path') VALUES ('three', 'три', 'q3');
                        INSERT INTO users ('username', 'created_at', 'chat_id') VALUES ('user1', CURRENT_TIMESTAMP, 1);
                        INSERT INTO users ('username', 'created_at', 'chat_id') VALUES ('user2', CURRENT_TIMESTAMP, 2);
                        INSERT INTO users ('username', 'created_at', 'chat_id') VALUES ('user3', CURRENT_TIMESTAMP, 3);
                        """.trimIndent()
                    )
                }
            }
        } catch (e: SQLException) {
            println("Ошибка: ${e.message}")
        }

        val dictionary = DatabaseUserDictionary(database)
        var text = Word("one", "один")
        var fileId = dictionary.getFilePath(text)
        val expectedBefore = "q1"
        val actualBefore = fileId

        kotlin.test.assertEquals(expectedBefore, actualBefore)

        text = Word("' UNION SELECT username FROM users ''", "один")
        fileId = dictionary.getFileId(text)
        val expectedAfter = null
        val actualAfter = fileId

        kotlin.test.assertEquals(expectedAfter, actualAfter)
    }
}
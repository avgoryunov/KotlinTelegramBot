package ru.avgoryunov.learnWordsBot.dictionary

import ru.avgoryunov.learnWordsBot.dictionary.data.TableStructure
import ru.avgoryunov.learnWordsBot.trainer.model.Word
import java.io.File
import java.sql.DriverManager
import java.sql.SQLException
import kotlin.use

class DatabaseUserDictionary(
    val database: String = DEFAULT_DATABASE_NAME,
    val learningThreshold: Int = DEFAULT_LEARNING_THRESHOLD,
) : IUserDictionary {
    override fun checkTheDatabaseStructure(): Boolean {
        val databaseStructure =
            listOf(
                TableStructure("words", listOf("id", "text", "translate")),
                TableStructure("users", listOf("id", "username", "created_at", "chat_id")),
                TableStructure("user_answers", listOf("user_id", "word_id", "correct_answer_count", "updated_at")),
            )

        var checkTheDatabase: Boolean
        val currentCheck = mutableListOf<Boolean>()

        return try {
            for (i in databaseStructure.indices) {
                DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
                    connection.createStatement().use { statement ->
                        statement.executeQuery(
                            """
                                SELECT EXISTS (SELECT name FROM sqlite_master
                                WHERE type = 'table' AND name = '${databaseStructure[i].table}')
                                """.trimIndent()
                        ).use { resultSet ->
                            currentCheck.add(resultSet.getBoolean(1))
                        }
                    }
                }

                for (i1 in databaseStructure[i].column.indices) {
                    DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
                        connection.createStatement().use { statement ->
                            statement.executeQuery(
                                """
                                    SELECT EXISTS (SELECT name FROM pragma_table_info('${databaseStructure[i].table}') 
                                    WHERE name = '${databaseStructure[i].column[i1]}')
                                    """.trimIndent()
                            ).use { resultSet ->
                                currentCheck.add(resultSet.getBoolean(1))
                            }
                        }
                    }
                }
            }

            DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery("SELECT COUNT(*) FROM ${databaseStructure[0].table}")
                        .use { resultSet ->
                            currentCheck.add(resultSet.getBoolean(1))
                        }
                }
            }

            checkTheDatabase = currentCheck.all { it }
            checkTheDatabase
        } catch (_: SQLException) {
            false
        }
    }

    override fun getNumberOfLearnedWords(chatId: Long?): Int {
        var numberOfLearnedWords = 0
        try {
            DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery(
                        """
                            SELECT COUNT (*) AS number_of_learned_words FROM user_answers
                            WHERE correct_answer_count >= $learningThreshold
                            AND user_id = (SELECT (id) FROM users WHERE chat_id = $chatId)
                            """.trimIndent()
                    ).use { resultSet ->
                        numberOfLearnedWords = resultSet.getInt("number_of_learned_words")
                    }
                }
            }
        } catch (e: SQLException) {
            e.message
        }
        return numberOfLearnedWords
    }

    override fun getSize(): Int {
        var size = 0
        try {
            DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery("SELECT COUNT (*) AS word_count FROM words")
                        .use { resultSet ->
                            size = resultSet.getInt("word_count")
                        }
                }
            }
        } catch (e: SQLException) {
            e.message
        }
        return size
    }

    override fun getLearnedWords(chatId: Long?): List<Word> {
        val learnedWordlist = mutableListOf<Word>()
        try {
            DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery(
                        """
                            SELECT * FROM user_answers INNER JOIN words ON words.id = user_answers.word_id
                            WHERE correct_answer_count >= $learningThreshold
                            AND user_id = (SELECT (id) FROM users WHERE chat_id = $chatId)
                            """.trimIndent()
                    ).use { resultSet ->
                        while (resultSet.next()) {
                            learnedWordlist.add(
                                Word(
                                    original = resultSet.getString("text"),
                                    translate = resultSet.getString("translate"),
                                    correctAnswersCount = resultSet.getInt("correct_answer_count")
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: SQLException) {
            e.message
        }
        return learnedWordlist
    }

    override fun getUnlearnedWords(chatId: Long?): List<Word> {
        val unlearnedWordlist = mutableListOf<Word>()
        try {
            DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery(
                        """
                            SELECT * FROM words LEFT JOIN user_answers ON user_answers.word_id = words.id
                            WHERE id NOT IN (SELECT word_id FROM user_answers 
                            WHERE user_id = (SELECT id FROM users WHERE chat_id = $chatId) AND correct_answer_count >= $learningThreshold)
                            """.trimIndent()
                    ).use { resultSet ->
                        while (resultSet.next()) {
                            unlearnedWordlist.add(
                                Word(
                                    original = resultSet.getString("text"),
                                    translate = resultSet.getString("translate"),
                                    correctAnswersCount = resultSet.getInt("correct_answer_count")
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: SQLException) {
            e.message
        }
        return unlearnedWordlist
    }

    override fun setCorrectAnswersCount(chatId: Long?, original: String, correctAnswersCount: Int) {
        try {
            DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeUpdate(
                        """
                            INSERT INTO user_answers ('user_id', 'word_id', 'correct_answer_count', 'updated_at')
                            VALUES ((SELECT (id) FROM users WHERE chat_id = $chatId), (SELECT (id) FROM words WHERE text = '$original'), $correctAnswersCount, CURRENT_TIMESTAMP)
                            ON CONFLICT DO UPDATE 
                            SET correct_answer_count = $correctAnswersCount, updated_at = CURRENT_TIMESTAMP
                            """.trimIndent()
                    )
                }
            }
        } catch (e: SQLException) {
            e.message
        }
    }

    override fun resetUserProgress(chatId: Long?) {
        try {
            DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeUpdate(
                        """
                            DELETE FROM user_answers 
                            WHERE user_id = (SELECT (id) FROM users WHERE chat_id = $chatId)
                            """.trimIndent()
                    )
                }
            }
        } catch (e: SQLException) {
            e.message
        }
    }

    override fun addNewUser(userName: String, chatId: Long?) {
        try {
            DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeUpdate(
                        """
                            INSERT INTO users ('username', 'created_at', 'chat_id')
                            VALUES ('$userName', CURRENT_TIMESTAMP, $chatId) ON CONFLICT DO NOTHING
                            """.trimIndent()
                    )
                }
            }
        } catch (e: SQLException) {
            e.message
        }
    }

    override fun updateTheDictionary(filename: String) {
        val updateList = mutableListOf<Word>()
        val updateFile = File(filename)
        updateFile.readLines().forEach {
            val splitLine = it.split("|")
            updateList.add(Word(splitLine[0], splitLine[1]))
        }

        for (i in updateList) {
            try {
                DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
                    connection.createStatement().use { statement ->
                        val wordCheck = statement.executeQuery(
                            "SELECT EXISTS (SELECT * FROM words WHERE text = '${i.original}')"
                        ).use { resultSet -> resultSet.getBoolean(1) }

                        if (!wordCheck) {
                            statement.executeUpdate(
                                "INSERT INTO words ('text', 'translate') VALUES ('${i.original}', '${i.translate}')"
                            )
                        }
                    }
                }
            } catch (e: SQLException) {
                e.message
            }
        }
    }
}

const val DEFAULT_DATABASE_NAME: String = "data.db"
const val DEFAULT_LEARNING_THRESHOLD = 3
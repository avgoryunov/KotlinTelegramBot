package ru.avgoryunov.learnWordsBot.dictionary

import ru.avgoryunov.learnWordsBot.trainer.model.Word
import java.sql.DriverManager
import kotlin.use

class DatabaseUserDictionary(
    val database: String = DEFAULT_DATABASE_NAME,
    val learningThreshold: Int = DEFAULT_LEARNING_THRESHOLD,
) : IUserDictionary {

    override fun getNumberOfLearnedWords(chatId: Long?): Int {
        var numberOfLearnedWords = 0
        DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(
                    """
                        SELECT COUNT (*) AS number_of_learned_words
                        FROM user_answers
                        WHERE correct_answer_count >= $learningThreshold
                        AND user_id = (SELECT (id) FROM users WHERE chat_id = $chatId)
                        """.trimIndent()
                ).use { resultSet ->
                    numberOfLearnedWords = resultSet.getInt("number_of_learned_words")
                }
            }
        }
        return numberOfLearnedWords
    }

    override fun getSize(): Int {
        var size = 0
        DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(
                    """
                        SELECT COUNT (*) AS word_count
                        FROM words
                        """.trimIndent()
                ).use { resultSet ->
                    size = resultSet.getInt("word_count")
                }
            }
        }
        return size
    }

    override fun getLearnedWords(chatId: Long?): List<Word> {
        val learnedWordlist = mutableListOf<Word>()
        DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(
                    """
                        SELECT * 
                        FROM user_answers INNER JOIN words ON words.id = user_answers.word_id
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
        return learnedWordlist
    }

    override fun getUnlearnedWords(chatId: Long?): List<Word> {
        val unlearnedWordlist = mutableListOf<Word>()
        DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(
                    """
                        SELECT *
                        FROM words LEFT JOIN user_answers ON user_answers.word_id = words.id
                        WHERE id <> COALESCE((
                        SELECT (id) 
                        FROM words LEFT JOIN user_answers ON user_answers.word_id = words.id 
                        WHERE (user_id = (
                        SELECT (id) 
                        FROM users 
                        WHERE chat_id = $chatId) 
                        AND correct_answer_count >= $learningThreshold)), 0)
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
        return unlearnedWordlist
    }

    override fun setCorrectAnswersCount(chatId: Long?, original: String, correctAnswersCount: Int) {
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
    }

    override fun resetUserProgress(chatId: Long?) {
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
    }

    override fun addNewUser(userName: String, chatId: Long?) {
        DriverManager.getConnection("jdbc:sqlite:data.db").use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(
                    """
                        INSERT INTO users ('username', 'created_at', 'chat_id')
                        VALUES ('$userName', CURRENT_TIMESTAMP, $chatId) ON CONFLICT DO NOTHING
                        """.trimIndent()
                )
            }
        }
    }
}

const val DEFAULT_DATABASE_NAME: String = "data.db"
const val DEFAULT_LEARNING_THRESHOLD = 3
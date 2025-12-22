package ru.avgoryunov.learnWordsBot.dictionary

import org.slf4j.LoggerFactory
import ru.avgoryunov.learnWordsBot.trainer.model.Word
import java.io.File
import java.sql.DriverManager
import java.sql.SQLException

class DatabaseUserDictionary(
    val database: String = DEFAULT_DATABASE_NAME,
    val learningThreshold: Int = DEFAULT_LEARNING_THRESHOLD,
) : IUserDictionary {
    override fun checkTheDatabaseStructure(): Boolean {
        var checkTheDatabase: Boolean
        val currentCheck = mutableListOf<Boolean>()

        return try {
            for (i in DatabaseStructure.entries.indices) {
                DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
                    val sql = """
                        SELECT EXISTS (SELECT name FROM sqlite_master
                        WHERE type = 'table' AND name = ?)
                        """.trimIndent()
                    connection.prepareStatement(sql).use { statement ->
                        statement.setString(1, DatabaseStructure.entries[i].tableName)
                        statement.executeQuery().use { resultSet ->
                            currentCheck.add(resultSet.getBoolean(1))
                        }
                    }
                }

                for (i1 in DatabaseStructure.entries[i].columnName.indices) {
                    DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
                        val sql = "SELECT EXISTS (SELECT name FROM pragma_table_info(?) WHERE name = ?)"
                        connection.prepareStatement(sql).use { statement ->
                            statement.setString(1, DatabaseStructure.entries[i].tableName)
                            statement.setString(2, DatabaseStructure.entries[i].columnName[i1])
                            statement.executeQuery().use { resultSet ->
                                currentCheck.add(resultSet.getBoolean(1))
                            }
                        }
                    }
                }
            }
            // Проверка наличия слов в таблице words словаря
            DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery("SELECT COUNT (*) FROM words").use { resultSet ->
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
        if (chatId != null) {
            try {
                DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
                    val sql = """
                    SELECT COUNT (*) FROM user_answers
                    WHERE correct_answer_count >= ?
                    AND user_id = (SELECT (id) FROM users WHERE chat_id = ?)
                    """.trimIndent()
                    connection.prepareStatement(sql).use { statement ->
                        statement.setInt(1, learningThreshold)
                        statement.setLong(2, chatId)
                        statement.executeQuery().use { resultSet ->
                            numberOfLearnedWords = resultSet.getInt(1)
                        }
                    }
                }
            } catch (e: SQLException) {
                e.message
            }
        }
        return numberOfLearnedWords
    }

    override fun getSize(): Int {
        var size = 0
        try {
            DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery("SELECT COUNT (*) FROM words").use { resultSet ->
                        size = resultSet.getInt(1)
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
        if (chatId != null) {
            try {
                DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
                    val sql = """
                        SELECT * FROM user_answers INNER JOIN words ON words.id = user_answers.word_id
                        WHERE correct_answer_count >= ?
                        AND user_id = (SELECT (id) FROM users WHERE chat_id = ?)
                        """.trimIndent()
                    connection.prepareStatement(sql).use { statement ->
                        statement.setInt(1, learningThreshold)
                        statement.setLong(2, chatId)
                        statement.executeQuery().use { resultSet ->
                            while (resultSet.next()) {
                                learnedWordlist.add(
                                    Word(
                                        original = resultSet.getString("text"),
                                        translate = resultSet.getString("translate"),
                                        correctAnswersCount = resultSet.getInt("correct_answer_count"),
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: SQLException) {
                e.message
            }
        }
        return learnedWordlist
    }

    override fun getUnlearnedWords(chatId: Long?): List<Word> {
        val unlearnedWordlist = mutableListOf<Word>()
        if (chatId != null) {
            try {
                DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
                    val sql = """
                        SELECT * FROM words LEFT JOIN user_answers ON user_answers.word_id = words.id
                        WHERE id NOT IN (SELECT word_id FROM user_answers WHERE correct_answer_count >= ?
                        AND user_id = (SELECT (id) FROM users WHERE chat_id = ?))
                        """.trimIndent()
                    connection.prepareStatement(sql).use { statement ->
                        statement.setInt(1, learningThreshold)
                        statement.setLong(2, chatId)
                        statement.executeQuery().use { resultSet ->
                            while (resultSet.next()) {
                                unlearnedWordlist.add(
                                    Word(
                                        original = resultSet.getString("text"),
                                        translate = resultSet.getString("translate"),
                                        correctAnswersCount = resultSet.getInt("correct_answer_count"),
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: SQLException) {
                e.message
            }
        }
        return unlearnedWordlist
    }

    override fun setCorrectAnswersCount(chatId: Long?, original: String, correctAnswersCount: Int) {
        if (chatId != null) {
            try {
                DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
                    loggingInputData(original)
                    val validatedOriginal = validateInputData(original)
                    val sql = """
                    INSERT INTO user_answers ('user_id', 'word_id', 'correct_answer_count', 'updated_at')
                    VALUES ((SELECT (id) FROM users WHERE chat_id = ?), (SELECT (id) FROM words WHERE text = ?), ?, CURRENT_TIMESTAMP)
                    ON CONFLICT DO UPDATE SET correct_answer_count = ?, updated_at = CURRENT_TIMESTAMP
                    """.trimIndent()
                    connection.prepareStatement(sql).use { statement ->
                        statement.setLong(1, chatId)
                        statement.setString(2, validatedOriginal)
                        statement.setInt(3, correctAnswersCount)
                        statement.setInt(4, correctAnswersCount)
                        statement.executeUpdate()
                    }
                }
            } catch (e: SQLException) {
                println(e.message)
            } catch (e: IllegalArgumentException) {
                println(e.message)
            }
        }
    }

    override fun resetUserProgress(chatId: Long?) {
        if (chatId != null) {
            try {
                DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
                    val sql = """
                    DELETE FROM user_answers 
                    WHERE user_id = (SELECT (id) FROM users WHERE chat_id = ?)
                    """.trimIndent()
                    connection.prepareStatement(sql).use { statement ->
                        statement.setLong(1, chatId)
                        statement.executeUpdate()
                    }
                }
            } catch (e: SQLException) {
                e.message
            }
        }
    }

    override fun addNewUser(chatId: Long?, userName: String) {
        if (chatId != null) {
            try {
                DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
                    loggingInputData(userName)
                    val validatedUserName = validateInputData(userName)
                    val sql = """
                    INSERT INTO users ('username', 'created_at', 'chat_id')
                    VALUES (?, CURRENT_TIMESTAMP, ?) ON CONFLICT DO NOTHING
                    """.trimIndent()
                    connection.prepareStatement(sql).use { statement ->
                        statement.setString(1, validatedUserName)
                        statement.setLong(2, chatId)
                        statement.executeUpdate()
                    }
                }
            } catch (e: SQLException) {
                println(e.message)
            } catch (e: IllegalArgumentException) {
                println(e.message)
            }
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
                    loggingInputData(i.original)
                    val validatedOriginal = validateInputData(i.original)
                    val sql = "SELECT EXISTS (SELECT * FROM words WHERE text = ?)"
                    connection.prepareStatement(sql).use { statement ->
                        statement.setString(1, validatedOriginal)
                        statement.executeQuery().use { resultSet ->

                            if (!resultSet.getBoolean(1)) {
                                loggingInputData(i.translate)
                                val validatedTranslate = validateInputData(i.translate)
                                val sql = "INSERT INTO words ('text', 'translate') VALUES (?, ?)"
                                connection.prepareStatement(sql).use { statement ->
                                    statement.setString(1, validatedOriginal)
                                    statement.setString(2, validatedTranslate)
                                    statement.executeUpdate()
                                }
                            }
                        }
                    }
                }
            } catch (e: SQLException) {
                println(e.message)
            } catch (e: IllegalArgumentException) {
                println(e.message)
            }
        }

        try {
            DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
                val wordIdList = mutableListOf<Int>()
                connection.createStatement().use { statement ->
                    statement.executeUpdate("UPDATE words SET photo_file_path = NULL")
                    statement.executeQuery("SELECT id FROM words ORDER BY id").use { resultSet ->
                        while (resultSet.next()) {
                            wordIdList.add(resultSet.getInt(1))
                        }
                    }
                }

                for (i in 1..<wordIdList.count()) {
                    val sql = "SELECT text FROM words WHERE id = ?"
                    val word = connection.prepareStatement(sql).use { statement ->
                        statement.setInt(1, wordIdList[i])
                        statement.executeQuery().use { resultSet ->
                            resultSet.getString(1)
                        }
                    }
                    val photoFileExists = File("photo/$word.png").exists()

                    if (photoFileExists) {
                        loggingInputData(word)
                        val validatedWord = validateInputData(word)
                        val sql = "UPDATE words SET photo_file_path = (?) WHERE text = ?"
                        connection.prepareStatement(sql).use { statement ->
                            statement.setString(1, "photo/$validatedWord.png")
                            statement.setString(2, validatedWord)
                            statement.executeUpdate()
                        }
                    }
                }
            }
        } catch (e: SQLException) {
            println(e.message)
        } catch (e: IllegalArgumentException) {
            println(e.message)
        }
    }

    override fun getFilePath(text: Word): String? {
        return try {
            DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
                loggingInputData(text.original)
                val validatedOriginal = validateInputData(text.original)
                val sql = "SELECT nullif(trim((SELECT photo_file_path FROM words WHERE text = ?)),'') is not null"
                connection.prepareStatement(sql).use { statement ->
                    statement.setString(1, validatedOriginal)
                    statement.executeQuery().use { resultSet ->

                        if (resultSet.getBoolean(1)) {
                            val sql = "SELECT photo_file_path FROM words WHERE text = ?"
                            connection.prepareStatement(sql).use { statement ->
                                statement.setString(1, validatedOriginal)
                                statement.executeQuery().use { resultSet -> resultSet.getString(1) }
                            }
                        } else null
                    }
                }
            }
        } catch (e: SQLException) {
            println(e.message)
            null
        } catch (e: IllegalArgumentException) {
            println(e.message)
            null
        }
    }

    override fun setFileId(text: Word, fileId: String?) {
        try {
            DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
                if (fileId != null) {
                    loggingInputData(fileId)
                    val validatedFileId = validateInputData(fileId)
                    loggingInputData(text.original)
                    val validatedOriginal = validateInputData(text.original)
                    val sql = "UPDATE words SET photo_file_id = (?) WHERE text = ?"
                    connection.prepareStatement(sql).use { statement ->
                        statement.setString(1, validatedFileId)
                        statement.setString(2, validatedOriginal)
                        statement.executeUpdate()
                    }
                }
            }
        } catch (e: SQLException) {
            println(e.message)
        } catch (e: IllegalArgumentException) {
            println(e.message)
        }
    }

    override fun getFileId(text: Word): String? {
        return try {
            DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
                loggingInputData(text.original)
                val validatedOriginal = validateInputData(text.original)
                val sql = "SELECT nullif(trim((SELECT photo_file_id FROM words WHERE text = ?)),'') is not null"
                connection.prepareStatement(sql).use { statement ->
                    statement.setString(1, validatedOriginal)
                    statement.executeQuery().use { resultSet ->

                        if (resultSet.getBoolean(1)) {
                            val sql = "SELECT photo_file_id FROM words WHERE text = ?"
                            connection.prepareStatement(sql).use { statement ->
                                statement.setString(1, validatedOriginal)
                                statement.executeQuery().use { resultSet ->
                                    resultSet.getString(1)
                                }
                            }
                        } else null
                    }
                }
            }
        } catch (e: SQLException) {
            println(e.message)
            null
        } catch (e: IllegalArgumentException) {
            println(e.message)
            null
        }
    }

    override fun getFilePathForEmptyPhoto(): String {
        return try {
            DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery("SELECT empty_photo_file_path FROM data").use { resultSet ->
                        resultSet.getString(1)
                    }
                }
            }
        } catch (e: SQLException) {
            e.message
        }!!
    }

    override fun setFileIdForEmptyPhoto(fileId: String?) {
        try {
            DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
                if (fileId != null) {
                    loggingInputData(fileId)
                    val validatedFileIdForEmptyPhoto = validateInputData(fileId)
                    val sql = "UPDATE data SET empty_photo_file_id = (?)"
                    connection.prepareStatement(sql).use { statement ->
                        statement.setString(1, validatedFileIdForEmptyPhoto)
                        statement.executeUpdate()
                    }
                }
            }
        } catch (e: SQLException) {
            println(e.message)
        } catch (e: IllegalArgumentException) {
            println(e.message)
        }
    }

    override fun getFileIdForEmptyPhoto(): String? {
        return try {
            DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery("SELECT empty_photo_file_id FROM data").use { resultSet ->
                        resultSet.getString(1)
                    }
                }
            }
        } catch (e: SQLException) {
            e.message
            null
        }
    }

    override fun setMessageId(chatId: Long?, messageId: Long?, messageIdColumn: MessageIdColumn) {
        if (messageId != null && chatId != null) {
            try {
                DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
                    val sql = "UPDATE users SET ${messageIdColumn.columnName} = (?) WHERE chat_id = ?"
                    connection.prepareStatement(sql).use { statement ->
                        statement.setLong(1, messageId)
                        statement.setLong(2, chatId)
                        statement.executeUpdate()
                    }
                }
            } catch (e: SQLException) {
                e.message
            }
        }
    }

    override fun getMessageId(chatId: Long?, messageIdColumn: MessageIdColumn): Long? {
        return if (chatId != null) {
            try {
                DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
                    val sql = "SELECT ${messageIdColumn.columnName} FROM users WHERE chat_id = ?"
                    connection.prepareStatement(sql).use { statement ->
                        statement.setLong(1, chatId)
                        statement.executeQuery().use { resultSet -> resultSet.getLong(1) }
                    }
                }
            } catch (e: SQLException) {
                e.message
                null
            }
        } else null
    }
}

fun loggingInputData(inputData: String) {
    val logger = LoggerFactory.getLogger("MyApp")

    val suspiciousPatterns =
        listOf("'", "\'", ";", "--", "/*", "*/", "union", "select", "drop", "delete", "insert", "update")
    val containsSuspicious = suspiciousPatterns.any { inputData.lowercase().contains(it) }

    if (containsSuspicious) logger.warn("Подозрительный ввод обнаружен: $inputData")
}

fun validateInputData(inputData: String): String {
    val allowedPattern = Regex("^[a-zA-Zа-яА-Я0-9_\\s\\-]+$")

    if (!allowedPattern.matches(inputData)) {
        throw IllegalArgumentException("Недопустимые символы")
    }

    if (inputData.length > 100) {
        throw IllegalArgumentException("Слишком длинное название")
    }

    return inputData.trim()
}

enum class DatabaseStructure(val tableName: String, val columnName: List<String>) {
    WORDS("words", listOf("id", "text", "translate", "photo_file_path", "photo_file_id")),
    USERS(
        "users", listOf(
            "id", "username", "created_at", "chat_id", "message_id_with_statistics", "message_id_with_photo",
            "message_id_with_question",
        )
    ),
    USER_ANSWERS("user_answers", listOf("user_id", "word_id", "correct_answer_count", "updated_at")),
    DATA("data", listOf("empty_photo_file_path", "empty_photo_file_id")),
}

enum class MessageIdColumn(val columnName: String) {
    STATISTICS("message_id_with_statistics"),
    PHOTO("message_id_with_photo"),
    QUESTION("message_id_with_question"),
}

const val DEFAULT_DATABASE_NAME: String = "data.db"
const val DEFAULT_LEARNING_THRESHOLD = 3
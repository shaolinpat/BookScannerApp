package com.example.bookscannerapp

import android.util.Log
import org.mariadb.jdbc.Statement
import java.sql.Connection
import java.sql.DriverManager

import java.util.Properties
import java.io.FileInputStream

private val properties = Properties().apply {
    load(FileInputStream("config.properties"))
}
private val dbUser: String = properties.getProperty("db.username", "default-username")
private val dbPassword: String = properties.getProperty("db.password", "default-password")

class DatabaseHelper {

    // Use the credentials that worked in your test
    private val dbUrl = "jdbc:mariadb://192.168.68.122:3306/BookDatabase"

    // Establish a database connection
    fun connect(): Connection? {
        return try {
            DriverManager.getConnection(dbUrl, dbUser, dbPassword)
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Connection failed: ${e.message}")
            null
        }
    }

    // Insert a book into the database
    fun insertBook(
        isbn: String?,
        title: String,
        edition: String,
        publisher: String,
        publicationDate: String?,
        locationId: Int,
        isRead: Boolean,
        readDate: String?
    ) {
        val sql = """
        INSERT INTO Books (ISBN, Title, Edition, Publisher, PublicationDate, LocationID, IsRead, ReadDate)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    """
        connect()?.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, isbn)
                statement.setString(2, title)
                statement.setString(3, edition)
                statement.setString(4, publisher)
                statement.setString(5, publicationDate)
                statement.setInt(6, locationId)
                statement.setBoolean(7, isRead)
                statement.setString(8, readDate)
                statement.executeUpdate()
                Log.d("DatabaseHelper", "Book inserted successfully: $title")
            }
        }
    }


    fun insertAuthorIfNotExists(name: String): Int {
        val sqlFind = "SELECT AuthorID FROM Authors WHERE Name = ?"
        val sqlInsert = "INSERT INTO Authors (Name) VALUES (?)"

        connect()?.use { connection ->
            connection.prepareStatement(sqlFind).use { statement ->
                statement.setString(1, name)
                val resultSet = statement.executeQuery()
                if (resultSet.next()) {
                    return resultSet.getInt("AuthorID")
                }
            }

            connection.prepareStatement(sqlInsert).use { statement ->
                statement.setString(1, name)
                statement.executeUpdate()
                return statement.generatedKeys.getInt(1)
            }
        }
        return -1
    }

    fun associateBookWithAuthor(bookId: Int, authorId: Int) {
        val sql = "INSERT INTO Books_Authors (BookID, AuthorID) VALUES (?, ?)"
        connect()?.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setInt(1, bookId)
                statement.setInt(2, authorId)
                statement.executeUpdate()
            }
        }
    }


    fun insertBookWithAuthors(
        title: String,
        authors: List<Author>?,
        publisher: String,
        publicationDate: String?,
        locationId: Int,
        isRead: Boolean,
        readDate: String?,
        edition: String,
        isbn: String?
    ) {
        connect()?.use { connection ->
            connection.autoCommit = false
            try {
                val sql = """
                INSERT INTO Books (ISBN, Title, Edition, Publisher, PublicationDate, LocationID, IsRead, ReadDate)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """
                val bookId: Int
                connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { statement ->
                    statement.setString(1, isbn)
                    statement.setString(2, title)
                    statement.setString(3, edition)
                    statement.setString(4, publisher)
                    statement.setString(5, publicationDate)
                    statement.setInt(6, locationId)
                    statement.setBoolean(7, isRead)
                    statement.setString(8, readDate)
                    statement.executeUpdate()
                    val rs = statement.generatedKeys
                    rs.next()
                    bookId = rs.getInt(1)
                }

                // Insert authors and associate them with the book
                authors?.forEach { author ->
                    val authorId = insertAuthorIfNotExists(author.name ?: "Unknown Author")
                    associateBookWithAuthor(bookId, authorId)
                }

                connection.commit()
                Log.d("DatabaseHelper", "Book and authors inserted successfully: $title")
            } catch (e: Exception) {
                connection.rollback()
                Log.e("DatabaseHelper", "Error inserting book and authors: ${e.message}")
            }
        }
    }


    fun getAllBooks(): List<Book> {
        val books = mutableListOf<Book>()
        val query = "SELECT * FROM Books"
        connect()?.use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        books.add(
                            Book(
                                id = resultSet.getInt("BookID"),
                                isbn = resultSet.getString("ISBN"),
                                title = resultSet.getString("Title"),
                                edition = resultSet.getString("Edition"),
                                publisher = resultSet.getString("Publisher"),
                                publicationDate = resultSet.getString("PublicationDate"),
                                locationId = resultSet.getInt("LocationID"),
                                isRead = resultSet.getBoolean("IsRead"),
                                readDate = resultSet.getString("ReadDate")
                            )
                        )
                    }
                }
            }
        }
        return books
    }



}

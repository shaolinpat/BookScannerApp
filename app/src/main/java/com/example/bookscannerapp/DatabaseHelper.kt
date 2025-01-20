package com.example.bookscannerapp

import android.util.Log
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.util.Properties

class DatabaseHelper(private val dbUser: String, private val dbPassword: String) {
    private val dbUrl = "jdbc:mariadb://192.168.68.122:3306/BookDatabase"

    fun connect(): Connection? {
        Log.d("DatabaseHelper", "Connecting with dbUser: $dbUser and dbPassword: $dbPassword") // Add this log
        return try {
            DriverManager.getConnection(dbUrl, dbUser, dbPassword)
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Connection failed: ${e.message}")
            null
        }
    }

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
        val sqlFind = "SELECT AuthorID FROM Authors WHERE LastName = ?"
        val sqlInsert = "INSERT INTO Authors (LastName) VALUES (?)"

        connect()?.use { connection ->
            // Check if the author exists
            connection.prepareStatement(sqlFind).use { statement ->
                statement.setString(1, name)
                val resultSet = statement.executeQuery()
                if (resultSet.next()) {
                    return resultSet.getInt("AuthorID")
                }
            }

            // Insert new author
            connection.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS).use { statement ->
                statement.setString(1, name)
                statement.executeUpdate()
                val generatedKeys = statement.generatedKeys
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1)
                }
            }
        }
        throw Exception("Failed to insert or find author: $name")
    }

    fun insertBookAndReturnId(
        isbn: String?,
        title: String,
        edition: String,
        publisher: String,
        publicationDate: String?,
        locationId: Int,
        isRead: Boolean,
        readDate: String?
    ): Int {
        val sql = """
        INSERT INTO Books (ISBN, Title, Edition, Publisher, PublicationDate, LocationID, IsRead, ReadDate)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    """
        connect()?.use { connection ->
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
                if (rs.next()) {
                    return rs.getInt(1)
                }
            }
        }
        throw Exception("Failed to insert book and retrieve BookID for ISBN: $isbn")
    }



    fun insertBookWithAuthors(
        isbn: String?,
        title: String,
        edition: String,
        publisher: String,
        publicationDate: String?,
        locationId: Int,
        isRead: Boolean,
        readDate: String?,
        authors: List<Author>?
    ) {
        connect()?.use { connection ->
            connection.autoCommit = false
            try {
                // Insert the book
                val bookSql = """
                INSERT INTO Books (ISBN, Title, Edition, Publisher, PublicationDate, LocationID, IsRead, ReadDate)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """
                val bookId: Int
                connection.prepareStatement(bookSql, Statement.RETURN_GENERATED_KEYS).use { statement ->
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

                // Insert authors and create associations
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

    fun getBookIdByIsbn(isbn: String): Int {
        val sql = "SELECT BookID FROM Books WHERE ISBN = ?"
        connect()?.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, isbn)
                val resultSet = statement.executeQuery()
                if (resultSet.next()) {
                    return resultSet.getInt("BookID")
                }
            }
        }
        throw Exception("Book with ISBN $isbn not found")
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

}

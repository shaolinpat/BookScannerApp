import java.sql.Connection
import java.sql.DriverManager

import java.util.Properties
import java.io.FileInputStream

private val properties = Properties().apply {
    load(FileInputStream("config.properties"))
}
private val dbUser: String = properties.getProperty("db.username", "default-username")
private val dbPassword: String = properties.getProperty("db.password", "default-password")


fun main() {
    val dbUrl = "jdbc:mariadb://192.168.68.122:3306/BookDatabase"

    try {
        val connection: Connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)
        println("Connection successful!")
        connection.close()
    } catch (e: Exception) {
        println("Connection failed: ${e.message}")
    }
}

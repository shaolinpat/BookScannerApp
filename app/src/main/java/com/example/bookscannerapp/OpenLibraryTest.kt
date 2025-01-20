package com.example.bookscannerapp

import kotlinx.coroutines.runBlocking
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

fun main() {
    // Retrofit initialization
    val retrofit = Retrofit.Builder()
        .baseUrl("https://openlibrary.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api = retrofit.create(OpenLibraryApi::class.java)

    runBlocking {
        val isbn = "9780136891055" // Replace with any valid ISBN
        val call = api.getBookDetails("ISBN:$isbn")
        try {
            val response = call.execute()
            if (response.isSuccessful) {
                val bookDetails = response.body()?.get("ISBN:$isbn")
                if (bookDetails != null) {
                    println("Title: ${bookDetails.title ?: "Unknown Title"}")
                    println("Publishers: ${bookDetails.publishers?.joinToString { it.name ?: "Unknown" }}")
                    println("Publish Date: ${bookDetails.publishDate ?: "Unknown Date"}")
                    println("Edition: ${bookDetails.editionName ?: "Unknown Edition"}")
                } else {
                    println("No details found for ISBN $isbn.")
                }
            } else {
                println("API Error: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            println("Request failed: ${e.message}")
        }
    }
}

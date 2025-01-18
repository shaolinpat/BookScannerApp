package com.example.bookscannerapp

data class BookDetailsResponse(
    val title: String?,
    val authors: List<Author>?,
    val publishers: List<Publisher>?,
    val publish_date: String?,
    val edition_name: String?
)

data class Author(val name: String?)
data class Publisher(val name: String?)

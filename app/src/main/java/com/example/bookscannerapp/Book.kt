package com.example.bookscannerapp

data class Book(
    val id: Int,
    val isbn: String?,
    val title: String,
    val edition: String?,
    val publisher: String,
    val publicationDate: String?,
    val locationId: Int,
    val isRead: Boolean,
    val readDate: String?
)

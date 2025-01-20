package com.example.bookscannerapp

import com.google.gson.annotations.SerializedName

data class BookDetailsResponse(
    @SerializedName("isbn") val isbn: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("publish_date") val publishDate: String?,
    @SerializedName("edition_name") val editionName: String?,
    @SerializedName("authors") val authors: List<Author>?,
    @SerializedName("publishers") val publishers: List<Publisher>?
)

data class Author(
    @SerializedName("name") val name: String?
)

data class Publisher(
    @SerializedName("name") val name: String?
)
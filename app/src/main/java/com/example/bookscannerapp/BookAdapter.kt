package com.example.bookscannerapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class BookAdapter(private val context: Context) : ListAdapter<Book, BookAdapter.BookViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Book>() {
            override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return BookViewHolder(view, context)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = getItem(position)
        holder.bind(book)
    }

    class BookViewHolder(itemView: View, private val context: Context) : RecyclerView.ViewHolder(itemView) {
        fun bind(book: Book) {
            val titleView = itemView.findViewById<TextView>(android.R.id.text1)
            val detailsView = itemView.findViewById<TextView>(android.R.id.text2)

            titleView.text = book.title
            detailsView.text = context.getString(
                R.string.book_details,
                book.publisher,
                book.publicationDate ?: "Unknown Date"
            )
        }
    }
}

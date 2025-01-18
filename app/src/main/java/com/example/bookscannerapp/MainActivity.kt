import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.example.bookscannerapp.R
import kotlinx.coroutines.Dispatchers

private lateinit var bookViewModel: BookViewModel

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    bookViewModel = ViewModelProvider(this).get(BookViewModel::class.java)

    bookViewModel.bookData.observe(this) { bookDetails ->
        if (bookDetails != null) {
            val title = bookDetails.title ?: "Unknown Title"
            val publisher = bookDetails.publishers?.joinToString(", ") { it.name ?: "Unknown Publisher" }
                ?: "Unknown Publisher"
            val edition = bookDetails.edition_name ?: "Unknown Edition"
            val publicationDate = formatPublicationDate(bookDetails.publish_date)

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    databaseHelper.insertBook(
                        title = title,
                        publisher = publisher,
                        publicationDate = publicationDate,
                        locationId = 1,
                        isRead = false,
                        readDate = null,
                        edition = edition,
                        isbn = bookDetails.isbn // Assuming your response has ISBN
                    )
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Book added: $title", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("Database", "Error inserting book: ${e.message}")
                }
            }
        } else {
            Toast.makeText(this, "Failed to fetch book details", Toast.LENGTH_SHORT).show()
        }
    }
}

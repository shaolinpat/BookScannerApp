import com.example.bookscannerapp.BookDetailsResponse
import com.example.bookscannerapp.OpenLibraryApi

class BookRepository(private val api: OpenLibraryApi) {
    suspend fun fetchBookDetails(isbn: String): BookDetailsResponse? {
        return try {
            val response = api.getBookDetails("ISBN:$isbn").execute()
            response.body()?.get("ISBN:$isbn")
        } catch (e: Exception) {
            null
        }
    }
}

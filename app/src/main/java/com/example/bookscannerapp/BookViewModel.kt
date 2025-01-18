import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookscannerapp.BookDetailsResponse
import com.example.bookscannerapp.OpenLibraryApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class BookViewModel : ViewModel() {
    private val repository: BookRepository
    private val _bookData = MutableLiveData<BookDetailsResponse?>()
    val bookData: LiveData<BookDetailsResponse?> get() = _bookData

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://openlibrary.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        repository = BookRepository(retrofit.create(OpenLibraryApi::class.java))
    }

    fun fetchBookDetails(isbn: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.fetchBookDetails(isbn)
            _bookData.postValue(result)
        }
    }
}

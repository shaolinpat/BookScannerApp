package com.example.bookscannerapp

import BookViewModel
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.Properties

class MainActivity : AppCompatActivity() {

    private lateinit var dbUser: String
    private lateinit var dbPassword: String
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var openLibraryApi: OpenLibraryApi
    private lateinit var bookViewModel: BookViewModel

    private val processingIsbns = mutableSetOf<String>() // Track ISBNs being processed

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraExecutor = Executors.newSingleThreadExecutor()
        loadConfigProperties()

        databaseHelper = DatabaseHelper(dbUser, dbPassword)
        openLibraryApi = createOpenLibraryApi()

        setupUIComponents()
        setupViewModel()
    }

    private fun loadConfigProperties() {
        val properties = Properties()
        try {
            assets.open("config.properties").use { inputStream ->
                properties.load(inputStream)
            }
            dbUser = properties.getProperty("db.username", "default-username").trim().removeSurrounding("\"")
            dbPassword = properties.getProperty("db.password", "default-password").trim().removeSurrounding("\"")
            Log.d("ConfigProperties", "Loaded username: $dbUser, password: $dbPassword")
        } catch (e: Exception) {
            Log.e("ConfigProperties", "Error loading config.properties: ${e.message}")
        }
    }

    private fun createOpenLibraryApi(): OpenLibraryApi {
        return Retrofit.Builder()
            .baseUrl("https://openlibrary.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenLibraryApi::class.java)
    }

    private fun setupUIComponents() {
        val isbnInput = findViewById<EditText>(R.id.isbnInput)
        val enterIsbnButton = findViewById<Button>(R.id.enterIsbnButton)
        val startScanButton = findViewById<Button>(R.id.startScanButton)

        enterIsbnButton.setOnClickListener {
            val isbn = isbnInput.text.toString()
            if (isbn.isNotBlank()) {
                Log.d("MainActivity", "Submitting ISBN: $isbn")
                fetchBookDetails(isbn)
            } else {
                Toast.makeText(this, "Please enter a valid ISBN", Toast.LENGTH_SHORT).show()
            }
        }

        startScanButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                requestCameraPermission()
            }
        }
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        }
    }

    private fun startCamera() {
        val previewView = findViewById<PreviewView>(R.id.previewView)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalyzer = ImageAnalysis.Builder().build().also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
                Log.d("MainActivity", "Camera started successfully")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error starting camera: ${e.message}", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_EAN_13)
                .build()

            val scanner = BarcodeScanning.getClient(options)

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val isbn = barcode.rawValue
                        Log.d("Barcode", "Scanned ISBN: $isbn")
                        if (isbn != null && isbn !in processingIsbns) {
                            processingIsbns.add(isbn)
                            fetchBookDetails(isbn)
                        }
                    }
                }
                .addOnFailureListener {
                    Log.e("Barcode", "Failed to process image: ${it.message}")
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }

    private fun fetchBookDetails(isbn: String) {
        Log.d("FetchBookDetails", "Fetching details for ISBN: $isbn")

        // Disable the submit button temporarily
        findViewById<Button>(R.id.enterIsbnButton).isEnabled = false

        openLibraryApi.getBookDetails("ISBN:$isbn").enqueue(object : Callback<Map<String, BookDetailsResponse>> {
            override fun onResponse(call: Call<Map<String, BookDetailsResponse>>, response: Response<Map<String, BookDetailsResponse>>) {
                if (response.isSuccessful) {
                    val bookDetails = response.body()?.get("ISBN:$isbn")
                    if (bookDetails != null) {
                        handleBookDetails(bookDetails, isbn)
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "No book details found", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Log.e("FetchBookDetails", "Failed to fetch book details: ${response.errorBody()?.string()}")
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Failed to fetch book details", Toast.LENGTH_SHORT).show()
                    }
                }
                // Re-enable the button
                findViewById<Button>(R.id.enterIsbnButton).isEnabled = true
            }

            override fun onFailure(call: Call<Map<String, BookDetailsResponse>>, t: Throwable) {
                Log.e("FetchBookDetails", "API call failed: ${t.message}")
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "API call failed", Toast.LENGTH_SHORT).show()
                }
                // Re-enable the button
                findViewById<Button>(R.id.enterIsbnButton).isEnabled = true
            }
        })
    }


    private fun handleBookDetails(bookDetails: BookDetailsResponse, isbn: String) {
        val title = bookDetails.title ?: "Unknown Title"
        val publisher = bookDetails.publishers?.joinToString(", ") { it.name ?: "Unknown Publisher" } ?: "Unknown Publisher"
        val edition = bookDetails.editionName ?: "Unknown Edition"
        val publicationDate = formatPublicationDate(bookDetails.publishDate)
        val authors = bookDetails.authors ?: emptyList()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Insert book into the database
                val bookId = databaseHelper.insertBookAndReturnId(
                    isbn = isbn,
                    title = title,
                    edition = edition,
                    publisher = publisher,
                    publicationDate = publicationDate,
                    locationId = 1,
                    isRead = false,
                    readDate = null
                )

                // Insert authors and populate Books_Authors
                authors.forEach { author ->
                    val authorId = databaseHelper.insertAuthorIfNotExists(author.name ?: "Unknown Author")
                    databaseHelper.associateBookWithAuthor(bookId, authorId)
                }

                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Book added: $title", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("Database", "Error inserting book and authors: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Failed to add book", Toast.LENGTH_SHORT).show()
                }
            } finally {
                resetLastScannedIsbnAfterDelay(isbn)
            }
        }
    }



    private fun formatPublicationDate(rawDate: String?): String? {
        if (rawDate.isNullOrEmpty()) {
            Log.w("DateFormat", "Raw date is null or empty")
            return null
        }

        val formats = listOf(
            "MMM d, yyyy", // Jan 19, 2021
            "d MMM yyyy",  // 19 Jan 2021
            "MMM yyyy",    // Jan 2021
            "yyyy"         // 2021
        )

        for (format in formats) {
            try {
                val dateFormat = SimpleDateFormat(format, Locale.US)
                val date = dateFormat.parse(rawDate)
                if (date != null) {
                    val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    return outputFormat.format(date)
                }
            } catch (e: Exception) {
                Log.w("DateFormat", "Failed to parse date: $rawDate with format $format")
            }
        }

        Log.e("DateFormat", "Failed to parse date: $rawDate")
        return null
    }

    private fun resetLastScannedIsbnAfterDelay(isbn: String) {
        lifecycleScope.launch {
            delay(5000)
            processingIsbns.remove(isbn)
        }
    }

    private fun setupViewModel() {
        bookViewModel = ViewModelProvider(this).get(BookViewModel::class.java)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

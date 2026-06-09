package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.BookDao
import com.example.data.model.Category
import com.example.data.model.PdfBook
import com.example.data.repository.BookRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class SortOption(val displayName: String) {
    LAST_VISITED("آخرین بازدید"),
    MOST_PAGES("بیشترین صفحه"),
    HIGHEST_SIZE("بیشترین حجم"),
    BY_FOLDER("به ترتیب پوشه")
}

class BookViewModel(private val repository: BookRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("همه")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.LAST_VISITED)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    // Observable states from DB
    val dbCategories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNotes: StateFlow<List<com.example.data.model.BookNote>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _dbBooks = repository.allBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredBooks: StateFlow<List<PdfBook>> = combine(
        _dbBooks,
        _selectedCategory,
        _searchQuery,
        _sortOption
    ) { books, category, query, sort ->
        val filtered = books.filter { book ->
            val matchesCategory = when (category) {
                "همه" -> true
                "★ علاقه‌مندی‌ها" -> book.isFavorite
                else -> book.category == category
            }
            val matchesSearch = query.isEmpty() || book.title.contains(query, ignoreCase = true)
            matchesCategory && matchesSearch
        }.distinctBy { it.filePath } // Prevent duplicates on UI!

        when (sort) {
            SortOption.LAST_VISITED -> filtered.sortedByDescending { it.addedDate }
            SortOption.MOST_PAGES -> filtered.sortedByDescending { it.totalPages }
            SortOption.HIGHEST_SIZE -> filtered.sortedByDescending { it.fileSize }
            SortOption.BY_FOLDER -> filtered.sortedWith(compareBy({ it.category }, { it.title }))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active reading state
    private val _activeBook = MutableStateFlow<PdfBook?>(null)
    val activeBook: StateFlow<PdfBook?> = _activeBook.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _readingTheme = MutableStateFlow("Sepia") // Light, Sepia, Dark
    val readingTheme: StateFlow<String> = _readingTheme.asStateFlow()

    private val _isVerticalLayout = MutableStateFlow(false) // Horizontal pager vs Vertical list
    val isVerticalLayout: StateFlow<Boolean> = _isVerticalLayout.asStateFlow()

    // Pdf rendering properties
    private var currentFileDescriptor: ParcelFileDescriptor? = null
    private var currentRenderer: PdfRenderer? = null
    
    private val _activeBookPages = MutableStateFlow(0)
    val activeBookPages: StateFlow<Int> = _activeBookPages.asStateFlow()

    // For keeping page bitmaps cache for smooth scrolling / responsive swipes
    private val _currentPageBitmap = MutableStateFlow<Bitmap?>(null)
    val currentPageBitmap: StateFlow<Bitmap?> = _currentPageBitmap.asStateFlow()

    private val _isPageLoading = MutableStateFlow(false)
    val isPageLoading: StateFlow<Boolean> = _isPageLoading.asStateFlow()

    private var hasPrepopulated = false

    fun prepopulateSampleBooksIfEmpty(context: Context) {
        if (hasPrepopulated) return
        hasPrepopulated = true
        viewModelScope.launch {
            // Read list directly to prevent ongoing collection triggers
            val current = _dbBooks.value
            if (current.isEmpty()) {
                generateSampleLiteratureBooks(context)
            }
        }
    }

    private suspend fun generateSampleLiteratureBooks(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                // Book 1: Hafez Shirazi Poetry
                val hafezLines = listOf(
                    "صلاح کار کجا و من خراب کجا\nببین تفاوت ره از کجاست تا به کجا\nدلم ز صومعه بگرفت و خرقه سالوس\nکجاست دیر مغان و شراب ناب کجا",
                    "اگر آن ترک شیرازی به دست آرد دل ما را\nبه خال هندویش بخشم سمرقند و بخارا را\nبده ساقی می باقی که در جنت نخواهی یافت\nکنار آب رکن آباد و گلگشت مصلا را",
                    "به مژگان سیه کردی هزاران رخنه در دینم\nبیا کز چشم بیمارت هزاران درد برچینم\nالا ای پیر فرزانه مکن عیبم ز میخانه\nکه من در ترک پیمانه خراب از دور دیرینم"
                )
                val hafezPath = createSamplePdf(context, "دیوان غزلیات حافظ شیرازی", hafezLines)
                repository.insertBook(
                    PdfBook(
                        title = "غزلیات حافظ شیرازی",
                        filePath = hafezPath,
                        category = "شعر و ادبیات",
                        totalPages = 3,
                        fileSize = File(hafezPath).length()
                    )
                )

                // Book 2: The Little Prince (Story)
                val princeLines = listOf(
                    "بزرگ‌ترها هرگز خودشان چیزی را نمی‌فهمند...\nو برای بچه‌ها هم خسته‌کننده است که همواره به آنها توضیح بدهند.\nشازده کوچولو گفت: زندگی در روی زمین چطور است؟\nروباه پاسخ داد: پر از شگفتی‌هاست اگر چشم دل باز کنی.",
                    "اگر تو مرا اهلی کنی، هر دو به هم احتیاج خواهیم داشت.\nتو برای من در تمام دنیا یگانه خواهی شد\nو من نیز برای تو در تمام دنیا همتا نخواهم داشت.\nصدای پای تو برای من مانند موسیقی خواهد بود...",
                    "ارزش گل تو به قدر عمری است که به پای آن صرف کرده‌ای.\nانسان‌ها این حقیقت را فراموش کرده‌اند\nاما تو نباید فراموش کنی. تو تا زنده‌ای نسبت به چیزی که\nاهلی کرده‌ای مسئولی. تو مسئول گل خود هستی..."
                )
                val princePath = createSamplePdf(context, "داستان شازده کوچولو", princeLines)
                repository.insertBook(
                    PdfBook(
                        title = "داستان شازده کوچولو",
                        filePath = princePath,
                        category = "داستان و رمان",
                        totalPages = 3,
                        fileSize = File(princePath).length()
                    )
                )

                // Book 3: Educational JS Manual
                val jsLines = listOf(
                    "راهنمای مقدماتی برنامه‌نویسی جاوااسکریپت\nجاوااسکریپت زبانی قدرتمند، پویا و مفسری است.\nاز متغیرهای مدرن const و let به جای var استفاده کنید:\nconst maxLimit = 100;\nlet currentCount = 0;",
                    "توابع پیکانی (Arrow Functions) در جاوااسکریپت:\nconst greetUser = (userName) => {\n   return 'سلام ' + userName + ' به برنامه خوش آمدید!';\n};\nاین مفهوم خوانایی کد شما را دوچندان می‌کند.",
                    "مدیریت رویدادهای آسنکرون (Promises & Async/Await):\nasync function loadBookData() {\n   try {\n       const response = await fetch('/api/books');\n       const books = await response.json();\n       console.log(books);\n   } catch (error) {\n       console.error(\"خطا در دریافت اطلاعات\", error);\n   }\n}"
                )
                val jsPath = createSamplePdf(context, "آموزش مقدماتی جاوااسکریپت", jsLines)
                repository.insertBook(
                    PdfBook(
                        title = "راهنمای سریع جاوااسکریپت",
                        filePath = jsPath,
                        category = "آموزشی و درسی",
                        totalPages = 3,
                        fileSize = File(jsPath).length()
                    )
                )

            } catch (e: Exception) {
                Log.e("BookViewModel", "Failed to populate books: ${e.message}")
            }
        }
    }

    private fun createSamplePdf(context: Context, title: String, lines: List<String>): String {
        val file = File(context.filesDir, "${title.replace(" ", "_")}.pdf")
        if (file.exists()) return file.absolutePath

        val document = PdfDocument()
        val paintHeader = Paint().apply {
            color = Color.rgb(44, 62, 80)
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        val paintBody = Paint().apply {
            color = Color.rgb(52, 73, 94)
            textSize = 17f
            isAntiAlias = true
        }
        val paintFooter = Paint().apply {
            color = Color.GRAY
            textSize = 11f
            isAntiAlias = true
        }

        val itemsPerPage = 1
        val totalPages = lines.size
        for (i in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(600, 850, i + 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // Page Background styling
            canvas.drawColor(Color.rgb(250, 248, 243)) // Soft warm paper color

            // Border decoration
            val borderPaint = Paint().apply {
                color = Color.rgb(212, 175, 55) // Gold
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            canvas.drawRect(25f, 25f, 575f, 825f, borderPaint)

            // Header banner
            canvas.drawText(title, 50f, 80f, paintHeader)

            // Draw line boundary
            canvas.drawLine(50f, 100f, 550f, 100f, borderPaint)

            // Body text
            val textToDraw = lines[i]
            val textParagraphs = textToDraw.split("\n")
            var currentY = 160f
            for (paragraph in textParagraphs) {
                canvas.drawText(paragraph, 50f, currentY, paintBody)
                currentY += 45f
            }

            // Footer info
            canvas.drawText("کتاب‌خوان پی‌دی‌اف  |  صفحه ${i + 1} از $totalPages", 50f, 790f, paintFooter)

            document.finishPage(page)
        }

        try {
            val fos = FileOutputStream(file)
            document.writeTo(fos)
            fos.flush()
            fos.close()
        } catch (e: Exception) {
            Log.e("BookViewModel", "writeTo failed: ${e.message}")
        } finally {
            document.close()
        }

        return file.absolutePath
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setReadingTheme(theme: String) {
        _readingTheme.value = theme
    }

    fun setVerticalLayout(vertical: Boolean) {
        _isVerticalLayout.value = vertical
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun renameCategoryAndRemapBooks(category: Category, newName: String) {
        if (newName.isBlank() || category.name == newName) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                // Update books using this category first
                repository.updateBookCategories(category.name, newName)
                // Rename the category itself
                repository.renameCategory(category.id, newName)
            }
        }
    }

    fun deleteCategoryAndRemapBooks(category: Category) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val fallbackCategory = "سایر"
                if (category.name == fallbackCategory) {
                    val alternative = dbCategories.value.firstOrNull { it.id != category.id }?.name ?: "سند"
                    repository.insertCategory(Category(name = alternative))
                    repository.updateBookCategories(category.name, alternative)
                } else {
                    repository.insertCategory(Category(name = fallbackCategory))
                    repository.updateBookCategories(category.name, fallbackCategory)
                }
                repository.deleteCategoryById(category.id)
            }
        }
    }

    fun openBookAtPage(context: Context, book: PdfBook, pageIndex: Int) {
        viewModelScope.launch {
            closeRenderer() // clear previous opened book
            
            val updatedBook = book.copy(addedDate = System.currentTimeMillis(), lastPageRead = pageIndex)
            _activeBook.value = updatedBook
            _currentPage.value = pageIndex

            withContext(Dispatchers.IO) {
                try {
                    repository.updateBook(updatedBook)
                    val file = File(updatedBook.filePath)
                    if (file.exists()) {
                        val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                        currentFileDescriptor = fd
                        val renderer = PdfRenderer(fd)
                        currentRenderer = renderer
                        _activeBookPages.value = renderer.pageCount
                        
                        // Load initial page bitmap
                        loadPageBitmap(pageIndex)
                    } else {
                        Log.e("BookViewModel", "File does not exist: ${updatedBook.filePath}")
                    }
                } catch (e: Exception) {
                    Log.e("BookViewModel", "Failed to load PDF at pageIndex $pageIndex: ${e.message}")
                }
            }
        }
    }

    fun openBook(context: Context, book: PdfBook) {
        viewModelScope.launch {
            closeRenderer() // clear previous opened book
            
            // To update last opened date (آخرین بازدید), copy and update book
            val updatedBook = book.copy(addedDate = System.currentTimeMillis())
            _activeBook.value = updatedBook
            _currentPage.value = updatedBook.lastPageRead

            withContext(Dispatchers.IO) {
                try {
                    repository.updateBook(updatedBook)
                    val file = File(updatedBook.filePath)
                    if (file.exists()) {
                        val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                        currentFileDescriptor = fd
                        val renderer = PdfRenderer(fd)
                        currentRenderer = renderer
                        _activeBookPages.value = renderer.pageCount
                        
                        // Load initial page bitmap
                        loadPageBitmap(updatedBook.lastPageRead)
                    } else {
                        Log.e("BookViewModel", "File does not exist: ${updatedBook.filePath}")
                    }
                } catch (e: Exception) {
                    Log.e("BookViewModel", "Failed to load PDF: ${e.message}")
                }
            }
        }
    }

    suspend fun renderPageToBitmap(pageIndex: Int): Bitmap? = withContext(Dispatchers.IO) {
        try {
            currentRenderer?.let { renderer ->
                if (pageIndex in 0 until renderer.pageCount) {
                    val page = renderer.openPage(pageIndex)
                    
                    // Scale page display proportionally for high crispness
                    val densityScale = 1.6f
                    val width = (page.width * densityScale).toInt()
                    val height = (page.height * densityScale).toInt()
                    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    
                    val canvas = Canvas(bmp)
                    canvas.drawColor(Color.WHITE)
                    
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    bmp
                } else null
            }
        } catch (e: Exception) {
            Log.e("BookViewModel", "Error rendering continuous page $pageIndex: ${e.message}")
            null
        }
    }

    fun changePage(pageIndex: Int) {
        val total = _activeBookPages.value
        if (pageIndex in 0 until total) {
            _currentPage.value = pageIndex
            // Update db async
            viewModelScope.launch {
                _activeBook.value?.let { book ->
                    repository.updateLastPageRead(book.id, pageIndex)
                }
                loadPageBitmap(pageIndex)
            }
        }
    }

    private suspend fun loadPageBitmap(pageIndex: Int) {
        _isPageLoading.value = true
        val bitmap = withContext(Dispatchers.IO) {
            try {
                currentRenderer?.let { renderer ->
                    if (pageIndex in 0 until renderer.pageCount) {
                        val page = renderer.openPage(pageIndex)
                        
                        // Scale the page display proportionally for high crispness
                        val densityScale = 1.6f
                        val width = (page.width * densityScale).toInt()
                        val height = (page.height * densityScale).toInt()
                        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        
                        val canvas = Canvas(bmp)
                        // Canvas default backdrop
                        canvas.drawColor(Color.WHITE)
                        
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()
                        bmp
                    } else null
                }
            } catch (e: Exception) {
                Log.e("BookViewModel", "Error rendering bitmap: ${e.message}")
                null
            }
        }
        _currentPageBitmap.value = bitmap
        _isPageLoading.value = false
    }

    fun toggleFavorite(book: PdfBook) {
        viewModelScope.launch {
            repository.updateBook(book.copy(isFavorite = !book.isFavorite))
        }
    }

    fun toggleBookmark() {
        val book = _activeBook.value ?: return
        val page = _currentPage.value
        viewModelScope.launch {
            val bookmarksList = if (book.bookmarks.isEmpty()) mutableListOf() else book.bookmarks.split(",").toMutableList()
            val pageStr = page.toString()
            if (bookmarksList.contains(pageStr)) {
                bookmarksList.remove(pageStr)
            } else {
                bookmarksList.add(pageStr)
            }
            val newBookmarksStr = bookmarksList.joinToString(",")
            val updated = book.copy(bookmarks = newBookmarksStr)
            repository.updateBook(updated)
            _activeBook.value = updated
        }
    }

    fun isPageBookmarked(pageIndex: Int): Boolean {
        val book = _activeBook.value ?: return false
        val bookmarksList = if (book.bookmarks.isEmpty()) emptyList() else book.bookmarks.split(",")
        return bookmarksList.contains(pageIndex.toString())
    }

    fun addCategory(name: String) {
        if (name.isNotBlank()) {
            viewModelScope.launch {
                repository.insertCategory(Category(name = name))
            }
        }
    }

    // Notes management APIs
    fun saveNote(bookId: Long, bookTitle: String, pageNumber: Int, content: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (content.isBlank()) {
                    val existing = repository.getNoteForBookPage(bookId, pageNumber)
                    if (existing != null) {
                        repository.deleteNoteById(existing.id)
                    }
                } else {
                    val existing = repository.getNoteForBookPage(bookId, pageNumber)
                    if (existing != null) {
                        repository.updateNote(existing.copy(content = content.trim(), timestamp = System.currentTimeMillis()))
                    } else {
                        repository.insertNote(
                            com.example.data.model.BookNote(
                                bookId = bookId,
                                bookTitle = bookTitle,
                                pageNumber = pageNumber,
                                content = content.trim()
                            )
                        )
                    }
                }
            }
        }
    }

    fun deleteNoteById(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteNoteById(id)
            }
        }
    }

    suspend fun getBookById(id: Long): PdfBook? {
        return repository.getBookById(id)
    }

    fun updateNoteContent(note: com.example.data.model.BookNote, newContent: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (newContent.isBlank()) {
                    repository.deleteNoteById(note.id)
                } else {
                    repository.updateNote(note.copy(content = newContent.trim(), timestamp = System.currentTimeMillis()))
                }
            }
        }
    }

    fun deleteCategory(name: String) {
        viewModelScope.launch {
            repository.deleteCategoryByName(name)
        }
    }

    fun importPdf(context: Context, uri: Uri, title: String, categoryName: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    // Create local file copy inside internal storage for speed and offline availability
                    val resolvedTitle = if (title.isBlank()) "سند جدید" else title
                    val cleanFileName = "${resolvedTitle.replace("\\s+".toRegex(), "_")}_${System.currentTimeMillis()}.pdf"
                    val destinationFile = File(context.filesDir, cleanFileName)

                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        FileOutputStream(destinationFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    if (destinationFile.exists()) {
                        // Scan file details
                        val parcelFileDescriptor = ParcelFileDescriptor.open(destinationFile, ParcelFileDescriptor.MODE_READ_ONLY)
                        val renderer = PdfRenderer(parcelFileDescriptor)
                        val pages = renderer.pageCount
                        renderer.close()
                        parcelFileDescriptor.close()

                        val book = PdfBook(
                            title = resolvedTitle,
                            filePath = destinationFile.absolutePath,
                            category = categoryName,
                            totalPages = pages,
                            fileSize = destinationFile.length()
                        )
                        repository.insertBook(book)
                    }
                } catch (e: Exception) {
                    Log.e("BookViewModel", "importPdf failed: ${e.message}")
                }
            }
        }
    }

    fun deleteBook(context: Context, book: PdfBook) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val file = File(book.filePath)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    Log.e("BookViewModel", "Failed to delete physical file: ${e.message}")
                }
                repository.deleteBookById(book.id)
            }
            if (_activeBook.value?.id == book.id) {
                closeBook()
            }
        }
    }

    fun closeBook() {
        _activeBook.value = null
        _currentPageBitmap.value = null
        closeRenderer()
    }

    private fun closeRenderer() {
        try {
            currentRenderer?.close()
        } catch (e: Exception) {
            // silent close
        }
        try {
            currentFileDescriptor?.close()
        } catch (e: Exception) {
            // silent close
        }
        currentRenderer = null
        currentFileDescriptor = null
    }

    override fun onCleared() {
        super.onCleared()
        closeRenderer()
    }
}

class BookViewModelFactory(private val repository: BookRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BookViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BookViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

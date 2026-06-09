package com.example.data.repository

import com.example.data.local.BookDao
import com.example.data.model.Category
import com.example.data.model.PdfBook
import com.example.data.model.BookNote
import kotlinx.coroutines.flow.Flow

class BookRepository(private val bookDao: BookDao) {
    val allBooks: Flow<List<PdfBook>> = bookDao.getAllBooks()
    val allCategories: Flow<List<Category>> = bookDao.getAllCategories()
    val allNotes: Flow<List<BookNote>> = bookDao.getAllNotes()

    fun getBooksByCategory(category: String): Flow<List<PdfBook>> {
        return if (category == "همه") {
            bookDao.getAllBooks()
        } else {
            bookDao.getBooksByCategory(category)
        }
    }

    fun getBookByIdFlow(id: Long): Flow<PdfBook?> {
        return bookDao.getBookByIdFlow(id)
    }

    suspend fun getBookById(id: Long): PdfBook? {
        return bookDao.getBookById(id)
    }

    suspend fun insertBook(book: PdfBook): Long {
        return bookDao.insertBook(book)
    }

    suspend fun updateBook(book: PdfBook) {
        bookDao.updateBook(book)
    }

    suspend fun updateLastPageRead(id: Long, page: Int) {
        bookDao.updateLastPageRead(id, page)
    }

    suspend fun deleteBookById(id: Long) {
        bookDao.deleteBookById(id)
        bookDao.deleteNotesByBookId(id)
    }

    suspend fun insertCategory(category: Category): Long {
        return bookDao.insertCategory(category)
    }

    suspend fun getCategoryByName(name: String): Category? {
        return bookDao.getCategoryByName(name)
    }

    suspend fun deleteCategoryByName(name: String) {
        bookDao.deleteCategoryByName(name)
    }

    suspend fun updateBookCategories(oldCategory: String, newCategory: String) {
        bookDao.updateBookCategories(oldCategory, newCategory)
    }

    suspend fun renameCategory(id: Long, newName: String) {
        bookDao.renameCategory(id, newName)
    }

    suspend fun deleteCategoryById(id: Long) {
        bookDao.deleteCategoryById(id)
    }

    // Book Notes APIs
    fun getNotesForBook(bookId: Long): Flow<List<BookNote>> {
        return bookDao.getNotesForBook(bookId)
    }

    suspend fun getNoteForBookPage(bookId: Long, pageNumber: Int): BookNote? {
        return bookDao.getNoteForBookPage(bookId, pageNumber)
    }

    suspend fun insertNote(note: BookNote): Long {
        return bookDao.insertNote(note)
    }

    suspend fun updateNote(note: BookNote) {
        bookDao.updateNote(note)
    }

    suspend fun deleteNoteById(id: Long) {
        bookDao.deleteNoteById(id)
    }
}

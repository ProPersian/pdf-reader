package com.example.data.repository

import com.example.data.local.BookDao
import com.example.data.model.Category
import com.example.data.model.PdfBook
import kotlinx.coroutines.flow.Flow

class BookRepository(private val bookDao: BookDao) {
    val allBooks: Flow<List<PdfBook>> = bookDao.getAllBooks()
    val allCategories: Flow<List<Category>> = bookDao.getAllCategories()

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
    }

    suspend fun insertCategory(category: Category): Long {
        return bookDao.insertCategory(category)
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
}

package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Category
import com.example.data.model.PdfBook
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    // PDF Book Operations
    @Query("SELECT * FROM pdf_books ORDER BY addedDate DESC")
    fun getAllBooks(): Flow<List<PdfBook>>

    @Query("SELECT * FROM pdf_books WHERE category = :category ORDER BY addedDate DESC")
    fun getBooksByCategory(category: String): Flow<List<PdfBook>>

    @Query("SELECT * FROM pdf_books WHERE id = :id")
    suspend fun getBookById(id: Long): PdfBook?

    @Query("SELECT * FROM pdf_books WHERE id = :id")
    fun getBookByIdFlow(id: Long): Flow<PdfBook?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: PdfBook): Long

    @Update
    suspend fun updateBook(book: PdfBook)

    @Query("DELETE FROM pdf_books WHERE id = :id")
    suspend fun deleteBookById(id: Long)

    @Query("UPDATE pdf_books SET lastPageRead = :page WHERE id = :id")
    suspend fun updateLastPageRead(id: Long, page: Int)

    // Category Operations
    @Query("SELECT * FROM categories ORDER BY id ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: Category): Long

    @Query("DELETE FROM categories WHERE name = :name")
    suspend fun deleteCategoryByName(name: String)
}

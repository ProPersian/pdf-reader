package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pdf_books")
data class PdfBook(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val filePath: String,
    val category: String, // e.g. "شعر", "داستانی", "درسی", "کارتونی" etc.
    val lastPageRead: Int = 0,
    val totalPages: Int = 0,
    val fileSize: Long = 0L,
    val addedDate: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val bookmarks: String = "" // Comma separated list of bookmarked page indexes, e.g. "2,5"
)

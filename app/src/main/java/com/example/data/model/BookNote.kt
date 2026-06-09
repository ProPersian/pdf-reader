package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "book_notes")
data class BookNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val bookTitle: String,
    val pageNumber: Int,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

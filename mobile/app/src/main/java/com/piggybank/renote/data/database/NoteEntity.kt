package com.piggybank.renotes.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: String,
    val kategori: String,
    val nominal: Int,
    val deskripsi: String,
    val tanggal: String
)


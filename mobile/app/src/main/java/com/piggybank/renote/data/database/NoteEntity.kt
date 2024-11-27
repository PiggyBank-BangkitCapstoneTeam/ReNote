package com.piggybank.renote.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val kategori: String,
    val nominal: Int,
    val deskripsi: String,
    val tanggal: String
)

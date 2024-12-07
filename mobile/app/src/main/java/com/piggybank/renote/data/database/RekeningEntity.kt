package com.piggybank.renotes.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rekening")
data class RekeningEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: String,
    val name: String,
    val uang: Int
)


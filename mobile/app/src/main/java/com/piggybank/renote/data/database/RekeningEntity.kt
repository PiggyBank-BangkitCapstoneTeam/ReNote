package com.piggybank.renote.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rekening")
data class RekeningEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val uang: Long
)

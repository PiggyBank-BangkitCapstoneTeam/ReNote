package com.piggybank.renote.data.database

import androidx.room.Dao
import androidx.room.Insert

@Dao
interface NoteDao {
    @Insert
    suspend fun insertNote(note: NoteEntity)
}

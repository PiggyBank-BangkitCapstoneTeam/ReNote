package com.piggybank.renote.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface NoteDao {
    // Catatan
    @Insert
    suspend fun insertNote(note: NoteEntity)

    @Query("SELECT * FROM notes WHERE tanggal = :date")
    suspend fun getNotesByDate(date: String): List<NoteEntity>

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    // Rekening
    @Insert
    suspend fun insertRekening(rekening: RekeningEntity)

    @Query("SELECT * FROM rekening")
    suspend fun getAllRekening(): List<RekeningEntity>

    @Update
    suspend fun updateRekening(rekening: RekeningEntity)

    @Delete
    suspend fun deleteRekening(rekening: RekeningEntity)

    // Laporan
    @Query("SELECT * FROM notes WHERE strftime('%m', tanggal) = :month AND strftime('%Y', tanggal) = :year")
    suspend fun getNotesByMonthAndYear(month: String, year: String): List<NoteEntity>

}

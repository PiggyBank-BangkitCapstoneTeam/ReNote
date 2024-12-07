package com.piggybank.renotes.ui.catatan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.piggybank.renotes.data.database.NoteDao
import com.piggybank.renotes.data.database.NoteEntity
import com.piggybank.renotes.data.database.NoteRoomDatabase
import kotlinx.coroutines.launch
import java.util.Calendar

class CatatanViewModel(application: Application) : AndroidViewModel(application) {

    private val noteDao: NoteDao = NoteRoomDatabase.getDatabase(application).noteDao()

    private val _catatanList = MutableLiveData<List<Catatan>>(emptyList())
    val catatanList: LiveData<List<Catatan>> = _catatanList

    private val _totalPemasukan = MutableLiveData(0)
    val totalPemasukan: LiveData<Int> = _totalPemasukan

    private val _totalPengeluaran = MutableLiveData(0)
    val totalPengeluaran: LiveData<Int> = _totalPengeluaran

    var selectedCatatan: Catatan? = null
    private var userId: String? = null

    private var saldoChangeListener: ((Int) -> Unit)? = null

    fun setUserId(id: String) {
        userId = id
    }

    fun updateDataForDate(date: Calendar) {
        val dateKey = getDateKey(date)
        userId?.let { user ->
            viewModelScope.launch {
                val notes = noteDao.getNotesByDateAndUser(dateKey, user)
                val catatanList = notes.map {
                    Catatan(it.kategori, it.nominal, it.deskripsi, it.tanggal)
                }
                _catatanList.postValue(catatanList)

                val pemasukan = catatanList.filter { it.nominal >= 0 }.sumOf { it.nominal }
                val pengeluaran = catatanList.filter { it.nominal < 0 }.sumOf { it.nominal }

                _totalPemasukan.postValue(pemasukan)
                _totalPengeluaran.postValue(pengeluaran)
            }
        } ?: error("User ID not set")
    }

    fun updateDataForMonthAll(month: String, year: String) {
        userId?.let { user ->
            viewModelScope.launch {
                val notes = noteDao.getNotesByMonthAndYear(month, year)
                    .filter { it.userId == user }
                val catatanList = notes.map {
                    Catatan(it.kategori, it.nominal, it.deskripsi, it.tanggal)
                }
                _catatanList.postValue(catatanList)

                val pemasukan = catatanList.filter { it.nominal >= 0 }.sumOf { it.nominal }
                val pengeluaran = catatanList.filter { it.nominal < 0 }.sumOf { it.nominal }

                _totalPemasukan.postValue(pemasukan)
                _totalPengeluaran.postValue(pengeluaran)
            }
        } ?: error("User ID not set")
    }


    fun addCatatan(date: Calendar, kategori: String, nominal: String, deskripsi: String) {
        val dateKey = getDateKey(date)
        val nominalValue = nominal.replace("[^\\d-]".toRegex(), "").toIntOrNull() ?: 0

        val newNote = NoteEntity(
            kategori = kategori,
            nominal = nominalValue,
            deskripsi = deskripsi,
            tanggal = dateKey,
            userId = userId ?: error("User ID not set")
        )

        viewModelScope.launch {
            noteDao.insertNote(newNote)
            updateDataForDate(date)
            saldoChangeListener?.invoke(nominalValue)
        }
    }

    fun editCatatan(newNominal: String, newDeskripsi: String) {
        selectedCatatan?.let { catatan ->
            val nominalValue = newNominal.replace("[^\\d-]".toRegex(), "").toIntOrNull() ?: return

            userId?.let { user ->
                viewModelScope.launch {
                    val notes = noteDao.getNotesByDateAndUser(catatan.tanggal, user)
                    val existingNote = notes.find {
                        it.deskripsi == catatan.deskripsi && it.kategori == catatan.kategori
                    }

                    existingNote?.let { noteEntity ->
                        val updatedNote = noteEntity.copy(
                            nominal = nominalValue,
                            deskripsi = newDeskripsi
                        )
                        noteDao.updateNote(updatedNote)

                        val updatedDate = Calendar.getInstance().apply {
                            val parts = updatedNote.tanggal.split("-")
                            set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                        }
                        updateDataForDate(updatedDate)
                    }
                }
            } ?: error("User ID not set")
        }
    }

    fun deleteSelectedCatatan(date: Calendar) {
        selectedCatatan?.let { catatan ->
            userId?.let { user ->
                viewModelScope.launch {
                    val notes = noteDao.getNotesByDateAndUser(catatan.tanggal, user)
                    val existingNote = notes.find {
                        it.deskripsi == catatan.deskripsi && it.kategori == catatan.kategori
                    }

                    existingNote?.let { noteEntity ->
                        noteDao.deleteNote(noteEntity)
                        updateDataForDate(date)
                        saldoChangeListener?.invoke(-catatan.nominal)
                        clearSelectedCatatan()
                    }
                }
            } ?: error("User ID not set")
        }
    }

    private fun getDateKey(date: Calendar): String {
        return "${date.get(Calendar.DAY_OF_MONTH)}-${date.get(Calendar.MONTH) + 1}-${date.get(Calendar.YEAR)}"
    }

    fun clearSelectedCatatan() {
        selectedCatatan = null
    }
}


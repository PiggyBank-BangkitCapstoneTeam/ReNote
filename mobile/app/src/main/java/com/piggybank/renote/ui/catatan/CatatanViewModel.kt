package com.piggybank.renotes.ui.catatan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.piggybank.renote.data.response.GetAllNoteResponse
import com.piggybank.renotes.data.database.NoteDao
import com.piggybank.renotes.data.database.NoteEntity
import com.piggybank.renotes.data.database.NoteRoomDatabase
import com.piggybank.renotes.data.pref.UserPreference
import com.piggybank.renotes.data.retrofit.ApiConfig
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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

    private fun syncDataWithServer(date: Calendar) {
        val token = UserPreference(getApplication()).getToken()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (token != null && userId != null) {
            val client = ApiConfig.getApiService(token)

            val selectedDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(date.time)

            viewModelScope.launch {
                client.getAllNotes().enqueue(object : Callback<GetAllNoteResponse> {
                    override fun onResponse(
                        call: Call<GetAllNoteResponse>,
                        response: Response<GetAllNoteResponse>
                    ) {
                        if (response.isSuccessful) {
                            val notes = response.body()?.data?.mapNotNull { dataItem ->
                                dataItem?.let {
                                    Catatan(
                                        id = it.id.toString(),
                                        kategori = it.kategori ?: "",
                                        nominal = it.nominal ?: 0,
                                        deskripsi = it.deskripsi ?: "",
                                        tanggal = it.tanggal ?: ""
                                    )
                                }
                            }?.filter { it.tanggal == selectedDateString } ?: emptyList()

                            val pemasukan = notes.filter { it.nominal >= 0 }.sumOf { it.nominal }
                            val pengeluaran = notes.filter { it.nominal < 0 }.sumOf { it.nominal }

                            _catatanList.postValue(notes)
                            _totalPemasukan.postValue(pemasukan)
                            _totalPengeluaran.postValue(pengeluaran)
                        }
                    }

                    override fun onFailure(call: Call<GetAllNoteResponse>, t: Throwable) {
                        // Handle failure
                    }
                })
            }
        }
    }

    fun updateDataForDate(date: Calendar) {
        syncDataWithServer(date)
    }

    fun updateDataForMonthAll(month: String, year: String) {
        userId?.let { user ->
            viewModelScope.launch {
                val notes = noteDao.getNotesByMonthAndYear(month, year)
                    .filter { it.userId == user }
                val catatanList = notes.map {
                    Catatan(
                        id = it.id.toString(),
                        kategori = it.kategori,
                        nominal = it.nominal,
                        deskripsi = it.deskripsi,
                        tanggal = it.tanggal
                    )
                }
                _catatanList.postValue(catatanList)

                val pemasukan = catatanList.filter { it.nominal >= 0 }.sumOf { it.nominal }
                val pengeluaran = catatanList.filter { it.nominal < 0 }.sumOf { it.nominal }

                _totalPemasukan.postValue(pemasukan)
                _totalPengeluaran.postValue(pengeluaran)
            }
        } ?: error("User ID not set")
    }


    fun addCatatan(date: Calendar, kategori: String, nominal: Int, deskripsi: String) {
        val dateKey = getDateKey(date)

        val newNote = NoteEntity(
            kategori = kategori,
            nominal = nominal,
            deskripsi = deskripsi,
            tanggal = dateKey,
            userId = userId ?: error("User ID not set")
        )

        viewModelScope.launch {
            noteDao.insertNote(newNote)
            updateDataForDate(date)
            saldoChangeListener?.invoke(nominal)
        }
    }

    fun editCatatan(newNominal: Int, newDeskripsi: String) {
        selectedCatatan?.let { catatan ->
            userId?.let { user ->
                viewModelScope.launch {
                    val notes = noteDao.getNotesByDateAndUser(catatan.tanggal, user)
                    val existingNote = notes.find {
                        it.deskripsi == catatan.deskripsi && it.kategori == catatan.kategori
                    }

                    existingNote?.let { noteEntity ->
                        val updatedNote = noteEntity.copy(
                            nominal = newNominal,
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
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(date.time)
    }

    fun clearSelectedCatatan() {
        selectedCatatan = null
    }
}


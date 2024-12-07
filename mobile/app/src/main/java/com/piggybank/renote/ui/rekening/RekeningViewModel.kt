package com.piggybank.renotes.ui.rekening

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.piggybank.renotes.data.database.NoteRoomDatabase
import com.piggybank.renotes.data.database.RekeningEntity
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class RekeningViewModel(application: Application) : AndroidViewModel(application) {

    private val noteDao = NoteRoomDatabase.getDatabase(application).noteDao()

    private val _rekeningList = MutableLiveData<List<Rekening>>()
    val rekeningList: LiveData<List<Rekening>> = _rekeningList

    private val _totalSaldo = MutableLiveData<Int>().apply { value = 0 }
    val totalSaldo: LiveData<Int> = _totalSaldo

    private val _activeRekening = MutableLiveData<Rekening?>()

    private var currentUserId: String? = null

    fun setUserId(userId: String) {
        currentUserId = userId
        loadRekeningForUser(userId)
    }

    fun setActiveRekening(rekening: Rekening) {
        _activeRekening.value = rekening
    }

    private fun loadRekeningForUser(userId: String) {
        viewModelScope.launch {
            val rekeningEntities = noteDao.getAllRekeningForUser(userId)
            if (rekeningEntities.isEmpty()) {
                insertDefaultRekening(userId)
            } else {
                _rekeningList.value = rekeningEntities.map { Rekening(it.name, it.uang) }
                _totalSaldo.value = rekeningEntities.sumOf { it.uang }
            }
        }
    }

    private suspend fun insertDefaultRekening(userId: String) {
        val defaultRekening = listOf(
            RekeningEntity(userId = userId, name = "DANA", uang = 0),
            RekeningEntity(userId = userId, name = "GoPay", uang = 0),
            RekeningEntity(userId = userId, name = "OVO", uang = 0),
            RekeningEntity(userId = userId, name = "LinkAja", uang = 0),
            RekeningEntity(userId = userId, name = "BCA", uang = 0),
            RekeningEntity(userId = userId, name = "BRI", uang = 0),
            RekeningEntity(userId = userId, name = "BNI", uang = 0),
            RekeningEntity(userId = userId, name = "Bank Mandiri", uang = 0)
        )
        defaultRekening.forEach { noteDao.insertRekening(it) }
        loadRekeningForUser(userId)
    }

    fun addRekening(rekening: Rekening): Boolean {
        val userId = currentUserId ?: return false
        val existingRekening = _rekeningList.value?.find { it.name.equals(rekening.name, ignoreCase = true) }
        if (existingRekening != null) return false

        viewModelScope.launch {
            noteDao.insertRekening(RekeningEntity(userId = userId, name = rekening.name, uang = rekening.uang))
            loadRekeningForUser(userId)
        }
        return true
    }

    fun updateRekening(updatedRekening: Rekening): Boolean {
        val userId = currentUserId ?: return false
        val currentList = _rekeningList.value?.toMutableList() ?: return false

        val index = currentList.indexOfFirst { it.name.equals(updatedRekening.name, ignoreCase = true) }
        if (index == -1) return false

        viewModelScope.launch {
            val rekeningEntity = noteDao.getAllRekeningForUser(userId)
                .find { it.name.equals(updatedRekening.name, ignoreCase = true) }
            if (rekeningEntity != null) {
                noteDao.updateRekening(
                    RekeningEntity(
                        id = rekeningEntity.id,
                        userId = userId,
                        name = updatedRekening.name,
                        uang = updatedRekening.uang
                    )
                )
                loadRekeningForUser(userId)
            }
        }
        return true
    }

    fun deleteRekening(rekening: Rekening): Boolean {
        val userId = currentUserId ?: return false
        val currentList = _rekeningList.value?.toMutableList() ?: return false

        val index = currentList.indexOfFirst { it.name.equals(rekening.name, ignoreCase = true) }
        if (index == -1) return false

        viewModelScope.launch {
            val rekeningEntity = noteDao.getAllRekeningForUser(userId)
                .find { it.name.equals(rekening.name, ignoreCase = true) }
            if (rekeningEntity != null) {
                noteDao.deleteRekening(rekeningEntity)
                loadRekeningForUser(userId)
            }
        }
        return true
    }

    fun formatCurrency(amount: Int): String {
        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }
        return format.format(amount).replace("Rp", "Rp.")
    }
}

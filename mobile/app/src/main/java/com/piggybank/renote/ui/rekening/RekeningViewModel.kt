package com.piggybank.renote.ui.rekening

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.piggybank.renote.data.database.NoteRoomDatabase
import com.piggybank.renote.data.database.RekeningEntity
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class RekeningViewModel(application: Application) : AndroidViewModel(application) {

    private val noteDao = NoteRoomDatabase.getDatabase(application).noteDao()

    private val _rekeningList = MutableLiveData<List<Rekening>>()
    val rekeningList: LiveData<List<Rekening>> = _rekeningList

    private val _totalSaldo = MutableLiveData<Int>().apply {
        value = _rekeningList.value?.sumOf { it.uang } ?: 0
    }
    val totalSaldo: LiveData<Int> = _totalSaldo

    private val _activeRekening = MutableLiveData<Rekening?>()

    fun setActiveRekening(rekening: Rekening) {
        _activeRekening.value = rekening
    }

    init {
        loadRekeningFromDatabase()
    }

    private fun loadRekeningFromDatabase() {
        viewModelScope.launch {
            val rekeningEntities = noteDao.getAllRekening()
            if (rekeningEntities.isEmpty()) {
                noteDao.insertRekening(RekeningEntity(name = "DANA", uang = 0))
                noteDao.insertRekening(RekeningEntity(name = "OVO", uang = 0))
                noteDao.insertRekening(RekeningEntity(name = "BCA", uang = 0))
            }
            val rekeningListFromDb = noteDao.getAllRekening().map { Rekening(it.name, it.uang) }
            _rekeningList.value = rekeningListFromDb
            _totalSaldo.value = rekeningListFromDb.sumOf { it.uang }
        }
    }

    fun addRekening(rekening: Rekening): Boolean {
        val existingRekening = _rekeningList.value?.find { it.name.equals(rekening.name, ignoreCase = true) }
        if (existingRekening != null) {
            return false
        }

        viewModelScope.launch {
            noteDao.insertRekening(RekeningEntity(name = rekening.name, uang = rekening.uang))

            val currentSaldo = noteDao.getAllRekening().sumOf { it.uang }
            setTotalSaldoDirectly(currentSaldo)

            loadRekeningFromDatabase()
        }
        return true
    }

    fun updateTotalSaldo(amount: String) {
        val amountValue = amount.toDoubleOrNull()?.toInt() ?: 0
        _totalSaldo.value = (_totalSaldo.value ?: 0) + amountValue
    }

    fun refreshTotalSaldo() {
        viewModelScope.launch {
            val currentSaldo = noteDao.getAllRekening().sumOf { it.uang }
            setTotalSaldoDirectly(currentSaldo)
        }
    }

    fun setTotalSaldoDirectly(amount: Int) {
        _totalSaldo.postValue(amount)
    }

    fun updateRekening(updatedRekening: Rekening): Boolean {
        val currentList = _rekeningList.value?.toMutableList() ?: return false

        val index = currentList.indexOfFirst { it.name.equals(updatedRekening.name, ignoreCase = true) }
        if (index == -1) {
            return false
        }

        viewModelScope.launch {
            val rekeningEntity = noteDao.getAllRekening().find { it.name.equals(updatedRekening.name, ignoreCase = true) }
            if (rekeningEntity != null) {
                noteDao.updateRekening(
                    RekeningEntity(
                        id = rekeningEntity.id,
                        name = updatedRekening.name,
                        uang = updatedRekening.uang
                    )
                )
            }

            val currentSaldo = noteDao.getAllRekening().sumOf { it.uang }
            setTotalSaldoDirectly(currentSaldo)

            loadRekeningFromDatabase()
        }

        return true
    }

    fun deleteRekening(rekening: Rekening): Boolean {
        val currentList = _rekeningList.value?.toMutableList() ?: return false

        val index = currentList.indexOfFirst { it.name.equals(rekening.name, ignoreCase = true) }
        if (index == -1) {
            return false
        }

        viewModelScope.launch {
            val rekeningEntity = noteDao.getAllRekening().find { it.name.equals(rekening.name, ignoreCase = true) }
            if (rekeningEntity != null) {
                noteDao.deleteRekening(rekeningEntity)
            }

            val currentSaldo = noteDao.getAllRekening().sumOf { it.uang }
            setTotalSaldoDirectly(currentSaldo)

            loadRekeningFromDatabase()
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

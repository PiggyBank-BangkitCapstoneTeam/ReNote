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

    private val _totalSaldo = MutableLiveData<Long>().apply {
        value = _rekeningList.value?.sumOf { it.uang } ?: 0L
    }
    val totalSaldo: LiveData<Long> = _totalSaldo

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
            _rekeningList.value = rekeningEntities.map { Rekening(it.name, it.uang) }
            updateTotalSaldo()
        }
    }

    fun addRekening(rekening: Rekening): Boolean {
        val existingRekening = _rekeningList.value?.find { it.name.equals(rekening.name, ignoreCase = true) }
        if (existingRekening != null) {
            return false
        }

        viewModelScope.launch {
            noteDao.insertRekening(RekeningEntity(name = rekening.name, uang = rekening.uang))
            loadRekeningFromDatabase()
        }
        return true
    }

    fun updateTotalSaldo(amount: String) {
        val amountValue = amount.toDoubleOrNull()?.toLong() ?: 0L
        _totalSaldo.value = (_totalSaldo.value ?: 0L) + amountValue
    }

    fun setTotalSaldoDirectly(amount: Long) {
        _totalSaldo.value = amount
    }

    private fun updateTotalSaldo() {
        _totalSaldo.value = _rekeningList.value?.sumOf { it.uang } ?: 0L
    }

    fun updateRekening(updatedRekening: Rekening): Boolean {
        val currentList = _rekeningList.value?.toMutableList() ?: return false

        val index = currentList.indexOfFirst { it.name.equals(updatedRekening.name, ignoreCase = true) }
        if (index == -1) {
            return false
        }

        currentList[index] = updatedRekening
        _rekeningList.value = currentList
        updateTotalSaldo()
        return true
    }

    fun deleteRekening(rekening: Rekening): Boolean {
        val currentList = _rekeningList.value?.toMutableList() ?: return false

        val index = currentList.indexOfFirst { it.name.equals(rekening.name, ignoreCase = true) }
        if (index == -1) {
            return false
        }

        _totalSaldo.value = _totalSaldo.value?.minus(currentList[index].uang)

        currentList.removeAt(index)
        _rekeningList.value = currentList
        return true
    }

    fun formatCurrency(amount: Long): String {
        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        return format.format(amount).replace("Rp", "Rp.")
    }
}

package com.piggybank.renote.ui.rekening

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.text.NumberFormat
import java.util.Locale

class RekeningViewModel : ViewModel() {

    private val _rekeningList = MutableLiveData<List<Rekening>>().apply {
        value = listOf(
            Rekening("DANA", 0L),
            Rekening("OVO", 0L),
            Rekening("BRI", 0L)
        )
    }
    val rekeningList: LiveData<List<Rekening>> = _rekeningList

    private val _totalSaldo = MutableLiveData<Long>().apply {
        value = _rekeningList.value?.sumOf { it.uang } ?: 0L
    }
    val totalSaldo: LiveData<Long> = _totalSaldo

    private val _activeRekening = MutableLiveData<Rekening?>()

    fun setActiveRekening(rekening: Rekening) {
        _activeRekening.value = rekening
    }

    fun addRekening(rekening: Rekening): Boolean {
        val existingRekening = _rekeningList.value?.find { it.name.equals(rekening.name, ignoreCase = true) }
        if (existingRekening != null) {
            return false
        }

        val updatedList = _rekeningList.value?.toMutableList() ?: mutableListOf()
        updatedList.add(rekening)
        _rekeningList.value = updatedList
        updateTotalSaldo()
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

package com.piggybank.renote.ui.catatan

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.piggybank.renote.ui.rekening.RekeningViewModel
import java.util.Calendar

class CatatanViewModel : ViewModel() {

    private val allData = mutableMapOf<String, MutableList<Catatan>>()

    private val _catatanList = MutableLiveData<List<Catatan>>(emptyList())
    val catatanList: LiveData<List<Catatan>> = _catatanList

    private val _totalPemasukan = MutableLiveData(0.0)
    val totalPemasukan: LiveData<Double> = _totalPemasukan

    private val _totalPengeluaran = MutableLiveData(0.0)
    val totalPengeluaran: LiveData<Double> = _totalPengeluaran

    private val _totalSaldo = MutableLiveData(0.0)
    val totalSaldo: LiveData<Double> = _totalSaldo

    var selectedCatatan: Catatan? = null

    var saldoChangeListener: ((Double) -> Unit)? = null

    fun updateDataForDate(date: Calendar) {
        val dateKey = getDateKey(date)
        val currentList = allData[dateKey]?.toMutableList() ?: mutableListOf()
        _catatanList.value = currentList

        val pemasukan = currentList.filter { it.nominal.toDouble() >= 0 }.sumOf { it.nominal.toDouble() }
        val pengeluaran = currentList.filter { it.nominal.toDouble() < 0 }.sumOf { it.nominal.toDouble() }
        _totalPemasukan.value = pemasukan
        _totalPengeluaran.value = pengeluaran
        _totalSaldo.value = pemasukan + pengeluaran
    }

    fun addCatatan(date: Calendar, kategori: String, nominal: String, deskripsi: String) {
        val dateKey = getDateKey(date)
        val nominalValue = nominal.replace("[^\\d.-]".toRegex(), "").toDoubleOrNull() ?: 0.0
        val formattedDate = "${date.get(Calendar.DAY_OF_MONTH)}-${date.get(Calendar.MONTH) + 1}-${date.get(Calendar.YEAR)}"

        val newCatatan = Catatan(kategori, nominalValue.toString(), deskripsi, formattedDate)

        val currentList = allData[dateKey] ?: mutableListOf()
        currentList.add(newCatatan)
        allData[dateKey] = currentList
        updateDataForDate(date)
    }

    fun editCatatan(date: Calendar, newNominal: String, newDeskripsi: String) {
        selectedCatatan?.let { catatan ->
            val dateKey = getDateKey(date)
            val currentList = allData[dateKey]?.toMutableList() ?: mutableListOf()
            val index = currentList.indexOf(catatan)
            if (index != -1) {
                val oldNominal = catatan.nominal.toDouble()
                val newNominalValue = newNominal.replace("[^\\d.-]".toRegex(), "").toDoubleOrNull() ?: 0.0

                currentList[index] = catatan.copy(nominal = newNominal, deskripsi = newDeskripsi)
                allData[dateKey] = currentList
                updateDataForDate(date)

                saldoChangeListener?.invoke(newNominalValue - oldNominal)
            }
        }
    }

    fun deleteSelectedCatatan(date: Calendar) {
        selectedCatatan?.let { catatan ->
            val dateKey = getDateKey(date)
            val currentList = allData[dateKey]?.toMutableList() ?: mutableListOf()
            if (currentList.remove(catatan)) {
                allData[dateKey] = currentList
                updateDataForDate(date)

                saldoChangeListener?.invoke(-catatan.nominal.toDouble())
            }
        }
    }

    fun calculateSaldo(): Double {
        return allData.values.flatten().sumOf { it.nominal.toDouble() }
    }

    fun refreshSaldo(rekeningViewModel: RekeningViewModel) {
        val totalSaldo = calculateSaldo()
        rekeningViewModel.setTotalSaldoDirectly(totalSaldo.toLong())
    }

    private fun getDateKey(date: Calendar): String {
        return "${date.get(Calendar.YEAR)}-${date.get(Calendar.MONTH) + 1}-${date.get(Calendar.DAY_OF_MONTH)}"
    }

    fun clearSelectedCatatan() {
        selectedCatatan = null
    }
}

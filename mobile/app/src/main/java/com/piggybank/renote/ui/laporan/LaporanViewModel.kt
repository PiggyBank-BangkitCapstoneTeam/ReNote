package com.piggybank.renote.ui.laporan

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LaporanViewModel : ViewModel() {

    private val _selectedDate = MutableLiveData<Pair<String, String>>()
    val selectedDate: LiveData<Pair<String, String>> = _selectedDate

    fun saveSelectedDate(month: String, year: String) {
        _selectedDate.value = month to year
    }
}
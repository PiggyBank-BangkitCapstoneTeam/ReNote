package com.piggybank.renote.ui.laporan

import MonthAdapter
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.piggybank.renote.R
import com.piggybank.renote.databinding.FragmentLaporanBinding
import com.piggybank.renote.ui.catatan.CatatanViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LaporanFragment : Fragment() {

    private var _binding: FragmentLaporanBinding? = null
    private val binding get() = _binding!!
    private lateinit var laporanViewModel: LaporanViewModel
    private val catatanViewModel: CatatanViewModel by lazy {
        ViewModelProvider(requireActivity())[CatatanViewModel::class.java]
    }

    private val monthMap = mapOf(
        "Jan" to "Januari",
        "Feb" to "Februari",
        "Mar" to "Maret",
        "Apr" to "April",
        "Mei" to "Mei",
        "Jun" to "Juni",
        "Jul" to "Juli",
        "Agust" to "Agustus",
        "Sept" to "September",
        "Okt" to "Oktober",
        "Nov" to "November",
        "Des" to "Desember"
    )

    private val pemasukanColors = listOf(
        Color.parseColor("#4CAF50"), Color.parseColor("#FFEB3B"),
        Color.parseColor("#2196F3"), Color.parseColor("#9C27B0")
    )

    private val pengeluaranColors = listOf(
        Color.parseColor("#F44336"), Color.parseColor("#FFC107"),
        Color.parseColor("#3F51B5"), Color.parseColor("#8BC34A"),
        Color.parseColor("#00BCD4"), Color.parseColor("#795548")
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        laporanViewModel = ViewModelProvider(this)[LaporanViewModel::class.java]

        _binding = FragmentLaporanBinding.inflate(inflater, container, false)
        val root: View = binding.root

        laporanViewModel.selectedDate.observe(viewLifecycleOwner) { (month, year) ->
            binding.dateDropdown.text = getString(R.string.date_display, month, year)
            lifecycleScope.launch {
                updatePieCharts()
            }
        }

        binding.dateDropdown.setOnClickListener {
            showMonthYearPicker()
        }

        binding.radioPemasukan.isChecked = true
        binding.pieChartPemasukan.visibility = View.VISIBLE
        binding.pieChartPengeluaran.visibility = View.GONE

        binding.toggleGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radio_pemasukan -> {
                    binding.pieChartPemasukan.visibility = View.VISIBLE
                    binding.pieChartPengeluaran.visibility = View.GONE
                }
                R.id.radio_pengeluaran -> {
                    binding.pieChartPemasukan.visibility = View.GONE
                    binding.pieChartPengeluaran.visibility = View.VISIBLE
                }
            }
        }

        observeCatatanChanges()

        return root
    }

    private fun observeCatatanChanges() {
        catatanViewModel.catatanList.observe(viewLifecycleOwner) {
            lifecycleScope.launch {
                updatePieCharts()
            }
        }
    }

    private fun convertMonthToNumber(month: String): Int {
        return when (month) {
            "Januari", "Jan" -> 1
            "Februari", "Feb" -> 2
            "Maret", "Mar" -> 3
            "April", "Apr" -> 4
            "Mei" -> 5
            "Juni", "Jun" -> 6
            "Juli", "Jul" -> 7
            "Agustus", "Agust" -> 8
            "September", "Sept" -> 9
            "Oktober", "Okt" -> 10
            "November", "Nov" -> 11
            "Desember", "Des" -> 12
            else -> 0
        }
    }

    private suspend fun updatePieCharts() = withContext(Dispatchers.Default) {
        val selectedDate = laporanViewModel.selectedDate.value ?: return@withContext
        val (selectedMonth, selectedYear) = selectedDate

        val filteredCatatan = catatanViewModel.catatanList.value?.filter { catatan ->
            val parts = catatan.tanggal.split("-")
            val year = parts[0].toIntOrNull()
            val month = parts[1].toIntOrNull()

            year == selectedYear.toIntOrNull() && month == convertMonthToNumber(selectedMonth)
        } ?: emptyList()


        val pemasukanCounts = mutableMapOf<String, Float>()
        val pengeluaranCounts = mutableMapOf<String, Float>()

        filteredCatatan.forEach { catatan ->
            val value = catatan.nominal.toDouble().toFloat()
            if (value >= 0) {
                pemasukanCounts[catatan.kategori] = (pemasukanCounts[catatan.kategori] ?: 0f) + value
            } else {
                pengeluaranCounts[catatan.kategori] = (pengeluaranCounts[catatan.kategori] ?: 0f) + -value
            }
        }

        // Persiapkan data untuk diagram pie
        val totalPemasukan = pemasukanCounts.values.sum()
        val totalPengeluaran = pengeluaranCounts.values.sum()

        val pemasukanData = pemasukanCounts.map { (kategori, count) ->
            PieEntry((count / totalPemasukan) * 100, kategori)
        }

        val pengeluaranData = pengeluaranCounts.map { (kategori, count) ->
            PieEntry((count / totalPengeluaran) * 100, kategori)
        }

        // Update UI dengan data baru
        withContext(Dispatchers.Main) {
            setupPieChart(binding.pieChartPemasukan, pemasukanData, pemasukanColors)
            setupPieChart(binding.pieChartPengeluaran, pengeluaranData, pengeluaranColors)
        }
    }

    private fun setupPieChart(pieChart: PieChart, data: List<PieEntry>, colors: List<Int>) {
        val dataSet = PieDataSet(data, "").apply {
            this.colors = colors
            sliceSpace = 2f
            valueTextSize = 12f
            valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return "${value.toInt()}%"
                }
            }
        }
        val pieData = PieData(dataSet).apply {
            setValueTextColor(Color.BLACK)
            setValueTextSize(12f)
        }
        pieChart.apply {
            this.data = pieData
            description.isEnabled = false
            isRotationEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 40f
            transparentCircleRadius = 50f
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(10f)
            extraBottomOffset = 10f
            extraTopOffset = 10f
            legend.orientation = Legend.LegendOrientation.HORIZONTAL
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            invalidate()
        }
    }

    private fun showMonthYearPicker() {
        val months = listOf(
            "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
            "Jul", "Agust", "Sept", "Okt", "Nov", "Des"
        )
        val years = (2010..2030).map { it.toString() }

        val currentSelection = laporanViewModel.selectedDate.value
        val defaultYear = currentSelection?.second ?: "2024"
        val defaultMonth = currentSelection?.first

        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_month_picker, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.monthRecyclerView)
        val yearSpinner = dialogView.findViewById<Spinner>(R.id.yearSpinner)

        var selectedMonth: String? = defaultMonth
        var selectedYear: String? = defaultYear

        val yearAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, years)
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        yearSpinner.adapter = yearAdapter
        yearSpinner.setSelection(years.indexOf(defaultYear))

        recyclerView.layoutManager = GridLayoutManager(context, 3)
        recyclerView.adapter = MonthAdapter(months) { month ->
            selectedMonth = month
            selectedYear = yearSpinner.selectedItem.toString()
        }

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                if (!selectedMonth.isNullOrEmpty() && !selectedYear.isNullOrEmpty()) {
                    val displayDate = (monthMap[selectedMonth] ?: selectedMonth) + " $selectedYear"
                    binding.dateDropdown.text = displayDate
                    laporanViewModel.saveSelectedDate(selectedMonth!!, selectedYear!!)
                    lifecycleScope.launch {
                        updatePieCharts()
                    }
                    Toast.makeText(context, "Dipilih: $displayDate", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Silakan pilih bulan dan tahun.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

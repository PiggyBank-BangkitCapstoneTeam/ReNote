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

class LaporanFragment : Fragment() {

    private var _binding: FragmentLaporanBinding? = null
    private val binding get() = _binding!!
    private lateinit var laporanViewModel: LaporanViewModel
    private val catatanViewModel: CatatanViewModel by lazy {
        ViewModelProvider(requireActivity()).get(CatatanViewModel::class.java)
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
        laporanViewModel = ViewModelProvider(this).get(LaporanViewModel::class.java)

        _binding = FragmentLaporanBinding.inflate(inflater, container, false)
        val root: View = binding.root

        laporanViewModel.selectedDate.observe(viewLifecycleOwner) { (month, year) ->
            binding.dateDropdown.text = "$month $year"
            updatePieCharts()
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
            updatePieCharts()
        }
    }

    private fun updatePieCharts() {
        val pemasukanCounts = mutableMapOf<String, Int>()
        val pengeluaranCounts = mutableMapOf<String, Int>()

        catatanViewModel.catatanList.value?.forEach { catatan ->
            if (catatan.nominal >= 0.toString()) {
                pemasukanCounts[catatan.kategori] = (pemasukanCounts[catatan.kategori] ?: 0) + 1
            } else {
                pengeluaranCounts[catatan.kategori] = (pengeluaranCounts[catatan.kategori] ?: 0) + 1
            }
        }


        val totalPemasukan = pemasukanCounts.values.sum().toFloat()
        val totalPengeluaran = pengeluaranCounts.values.sum().toFloat()

        val pemasukanData = pemasukanCounts.map { (kategori, count) ->
            PieEntry((count / totalPemasukan) * 100, kategori)
        }

        val pengeluaranData = pengeluaranCounts.map { (kategori, count) ->
            PieEntry((count / totalPengeluaran) * 100, kategori)
        }

        setupPieChart(binding.pieChartPemasukan, pemasukanData, pemasukanColors)
        setupPieChart(binding.pieChartPengeluaran, pengeluaranData, pengeluaranColors)
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

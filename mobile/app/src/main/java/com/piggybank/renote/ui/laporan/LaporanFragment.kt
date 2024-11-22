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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.piggybank.renote.R
import com.piggybank.renote.databinding.FragmentLaporanBinding

class LaporanFragment : Fragment() {

    private var _binding: FragmentLaporanBinding? = null
    private val binding get() = _binding!!
    private lateinit var laporanAdapter: LaporanAdapter
    private lateinit var laporanViewModel: LaporanViewModel

    private val monthMap = mapOf(
        "Jan" to "Januari", "Feb" to "Februari", "Mar" to "Maret",
        "Apr" to "April", "Mei" to "Mei", "Jun" to "Juni",
        "Jul" to "Juli", "Agust" to "Agustus", "Sept" to "September",
        "Okt" to "Oktober", "Nov" to "November", "Des" to "Desember"
    )

    private val pemasukanData = listOf(
        PieEntry(40f, "Gaji"), PieEntry(30f, "Investasi"),
        PieEntry(20f, "Paruh Waktu"), PieEntry(10f, "Lain-lain")
    )

    private val pengeluaranData = listOf(
        PieEntry(25f, "Belanja"), PieEntry(20f, "Makanan"),
        PieEntry(15f, "Minuman"), PieEntry(10f, "Pulsa"),
        PieEntry(20f, "Transportasi"), PieEntry(10f, "Lain-lain")
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
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        laporanViewModel = ViewModelProvider(this).get(LaporanViewModel::class.java)

        _binding = FragmentLaporanBinding.inflate(inflater, container, false)
        val root: View = binding.root

        laporanAdapter = LaporanAdapter(emptyList())
        binding.laporanList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = laporanAdapter
        }

        laporanViewModel.categoryList.observe(viewLifecycleOwner) { categories ->
            laporanAdapter.updateData(categories)
        }

        laporanViewModel.selectedDate.observe(viewLifecycleOwner) { (month, year) ->
            binding.dateDropdown.text = "${monthMap[month] ?: month} $year"
        }

        binding.dateDropdown.setOnClickListener {
            showMonthYearPicker()
        }

        setupPieChart(binding.pieChartPemasukan, pemasukanData, pemasukanColors)
        setupPieChart(binding.pieChartPengeluaran, pengeluaranData, pengeluaranColors)

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

        return root
    }

    private fun setupPieChart(pieChart: PieChart, data: List<PieEntry>, colors: List<Int>) {
        val dataSet = PieDataSet(data, "").apply {
            this.colors = colors
            sliceSpace = 2f
            valueTextSize = 12f
        }
        val pieData = PieData(dataSet).apply {
            setValueTextColor(Color.WHITE)
        }
        pieChart.apply {
            this.data = pieData
            description.isEnabled = false
            isRotationEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 40f
            transparentCircleRadius = 50f
            setEntryLabelColor(Color.BLACK)
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

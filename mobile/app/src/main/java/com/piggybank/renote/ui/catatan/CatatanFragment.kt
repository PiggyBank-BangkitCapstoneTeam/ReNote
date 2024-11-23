package com.piggybank.renote.ui.catatan

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.piggybank.renote.R
import com.piggybank.renote.databinding.FragmentCatatanBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class CatatanFragment : Fragment() {

    private var selectedDate: Calendar = Calendar.getInstance()
    private var _binding: FragmentCatatanBinding? = null
    private val binding get() = _binding!!

    private lateinit var catatanAdapter: CatatanAdapter
    private val catatanViewModel: CatatanViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCatatanBinding.inflate(inflater, container, false)

        catatanAdapter = CatatanAdapter { catatan ->
            lifecycleScope.launch {
                catatanViewModel.selectedCatatan = catatan
                withContext(Dispatchers.Main) {
                    findNavController().navigate(R.id.navigation_editCatatan)
                }
            }
        }

        binding.transactionRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = catatanAdapter
            val itemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            addItemDecoration(itemDecoration)
        }

        updateUIForDate(selectedDate)

        binding.catatanAdd.setOnClickListener {
            lifecycleScope.launch {
                catatanViewModel.clearSelectedCatatan()
                withContext(Dispatchers.Main) {
                    findNavController().navigate(R.id.navigation_tambahCatatan)
                }
            }
        }

        binding.calendarButton.setOnClickListener {
            showDatePickerDialog()
        }

        return binding.root
    }

    private fun updateUIForDate(date: Calendar) {
        lifecycleScope.launch {
            val dateKey = "${date.get(Calendar.YEAR)}-${date.get(Calendar.MONTH) + 1}-${date.get(Calendar.DAY_OF_MONTH)}"

            // Muat catatan dari Room Database
            catatanViewModel.getNotesByDateFromDatabase(dateKey).observe(viewLifecycleOwner) { noteEntities ->
                val catatanList = noteEntities.map { noteEntity ->
                    Catatan(
                        kategori = noteEntity.category,
                        nominal = noteEntity.nominal,
                        deskripsi = noteEntity.description,
                        tanggal = noteEntity.date,
                    )
                }
                catatanAdapter.submitList(catatanList)
            }

            // Perbarui pemasukan/pengeluaran
            catatanViewModel.updateDataForDate(date)
        }
    }


    private fun showDatePickerDialog() {
        val year = selectedDate.get(Calendar.YEAR)
        val month = selectedDate.get(Calendar.MONTH)
        val day = selectedDate.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                selectedDate = Calendar.getInstance().apply {
                    set(selectedYear, selectedMonth, selectedDay)
                }
                updateUIForDate(selectedDate)
            },
            year, month, day
        )

        datePickerDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

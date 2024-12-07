package com.piggybank.renotes.ui.catatan

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.piggybank.renotes.R
import com.piggybank.renotes.databinding.FragmentCatatanBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class CatatanFragment : Fragment() {

    private var selectedDate: Calendar = Calendar.getInstance()
    private var _binding: FragmentCatatanBinding? = null
    private val binding get() = _binding!!

    private lateinit var catatanAdapter: CatatanAdapter
    private val catatanViewModel: CatatanViewModel by activityViewModels()

    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

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
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

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

        updateUIForDate(selectedDate)

        return binding.root
    }

    private fun updateUIForDate(date: Calendar) {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            catatanViewModel.setUserId(currentUser.uid)
            catatanViewModel.updateDataForDate(date)

            catatanViewModel.catatanList.observe(viewLifecycleOwner) { catatanList ->
                lifecycleScope.launch {
                    withContext(Dispatchers.Main) {
                        catatanAdapter.submitList(catatanList)
                    }
                }
            }

            catatanViewModel.totalPemasukan.observe(viewLifecycleOwner) { pemasukan ->
                lifecycleScope.launch {
                    val formattedPemasukan =
                        NumberFormat.getNumberInstance(Locale.getDefault()).format(pemasukan)
                    withContext(Dispatchers.Main) {
                        binding.textPemasukan.text = getString(R.string.pemasukan_text, formattedPemasukan)
                    }
                }
            }

            catatanViewModel.totalPengeluaran.observe(viewLifecycleOwner) { pengeluaran ->
                lifecycleScope.launch {
                    val formattedPengeluaran =
                        NumberFormat.getNumberInstance(Locale.getDefault()).format(pengeluaran)
                    withContext(Dispatchers.Main) {
                        binding.textPengeluaran.text =
                            getString(R.string.pengeluaran_text, formattedPengeluaran)
                    }
                }
            }
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

    override fun onResume() {
        super.onResume()
        updateUIForDate(selectedDate)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

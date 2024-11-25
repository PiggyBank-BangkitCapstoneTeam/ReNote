package com.piggybank.renote.ui.catatan

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.piggybank.renote.R
import com.piggybank.renote.data.database.NoteEntity
import com.piggybank.renote.data.database.NoteRoomDatabase
import com.piggybank.renote.databinding.FragmentTambahBinding
import com.piggybank.renote.ui.rekening.RekeningViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TambahCatatan : Fragment() {

    private var _binding: FragmentTambahBinding? = null
    private val binding get() = _binding!!

    private val catatanViewModel: CatatanViewModel by activityViewModels()
    private val rekeningViewModel: RekeningViewModel by activityViewModels()

    private lateinit var database: NoteRoomDatabase
    private var selectedDate: Calendar? = null

    private val pemasukanCategory = listOf("Pilih Kategori", "Gaji", "Investasi", "Paruh Waktu", "Lain-lain")
    private val pengeluaranCategory = listOf("Pilih Kategori", "Belanja", "Makanan", "Minuman", "Pulsa", "Transportasi", "Lain-lain")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTambahBinding.inflate(inflater, container, false)
        database = NoteRoomDatabase.getDatabase(requireContext())

        val bottomNavigationView = requireActivity().findViewById<View>(R.id.nav_view)
        bottomNavigationView.visibility = View.GONE

        binding.iconBack.setOnClickListener {
            lifecycleScope.launch {
                withContext(Dispatchers.Main) {
                    bottomNavigationView.visibility = View.VISIBLE
                    findNavController().navigateUp()
                }
            }
        }

        binding.iconCalendar.setOnClickListener {
            showDatePickerDialog()
        }

        setupCategorySpinner(pengeluaranCategory)

        binding.toggleGroup.setOnCheckedChangeListener { _, checkedId ->
            lifecycleScope.launch {
                val categories = if (checkedId == R.id.radio_pengeluaran) pengeluaranCategory else pemasukanCategory
                withContext(Dispatchers.Main) {
                    setupCategorySpinner(categories)
                }
            }
        }

        binding.buttonCreate.setOnClickListener {
            lifecycleScope.launch {
                val kategori = binding.spinnerCategory.selectedItem.toString()
                val nominal = binding.inputAmount.text.toString()
                val deskripsi = binding.inputDescription.text.toString()
                val isPengeluaran = binding.toggleGroup.checkedRadioButtonId == R.id.radio_pengeluaran

                val adjustedNominal = if (isPengeluaran) "-$nominal" else nominal

                if (selectedDate == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Pilih tanggal terlebih dahulu!", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                if (kategori == "Pilih Kategori" || nominal.isBlank() || deskripsi.isBlank()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Isi semua data dengan benar!", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                try {
                    catatanViewModel.addCatatan(selectedDate!!, kategori, adjustedNominal, deskripsi)
                    rekeningViewModel.updateTotalSaldo(adjustedNominal)

                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val formattedDate = dateFormat.format(selectedDate!!.time)

                    val noteEntity = NoteEntity(
                        kategori = kategori,
                        nominal = adjustedNominal,
                        deskripsi = deskripsi,
                        tanggal = formattedDate
                    )
                    withContext(Dispatchers.IO) {
                        database.noteDao().insertNote(noteEntity)
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Catatan berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                        val bottomNavigationView = requireActivity().findViewById<View>(R.id.nav_view)
                        bottomNavigationView.visibility = View.VISIBLE
                        findNavController().navigateUp()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Terjadi kesalahan: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        return binding.root
    }

    private fun setupCategorySpinner(categories: List<String>) {
        lifecycleScope.launch {
            val adapter = withContext(Dispatchers.Default) {
                ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    categories
                ).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }
            }
            withContext(Dispatchers.Main) {
                binding.spinnerCategory.adapter = adapter
            }
        }
    }

    private fun showDatePickerDialog() {
        val calendar = selectedDate ?: Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                lifecycleScope.launch {
                    selectedDate = Calendar.getInstance().apply {
                        set(selectedYear, selectedMonth, selectedDay)
                    }
                    withContext(Dispatchers.Main) {
                        binding.textDate.text =
                            getString(R.string.date_format, selectedDay, selectedMonth + 1, selectedYear)
                    }
                }
            },
            year, month, day
        )

        datePickerDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                val bottomNavigationView = requireActivity().findViewById<View>(R.id.nav_view)
                bottomNavigationView.visibility = View.VISIBLE
                _binding = null
            }
        }
    }
}

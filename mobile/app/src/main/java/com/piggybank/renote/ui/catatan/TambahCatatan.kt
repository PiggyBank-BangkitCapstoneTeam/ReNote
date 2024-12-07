package com.piggybank.renotes.ui.catatan

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.piggybank.renotes.R
import com.piggybank.renotes.databinding.FragmentTambahBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class TambahCatatan : Fragment() {

    private var _binding: FragmentTambahBinding? = null
    private val binding get() = _binding!!

    private val catatanViewModel: CatatanViewModel by activityViewModels()

    private var selectedDate: Calendar? = null

    private val pemasukanCategory = listOf("Pilih Kategori", "Gaji", "Investasi", "Paruh Waktu", "Lain-lain")
    private val pengeluaranCategory = listOf("Pilih Kategori", "Parkir", "Makanan dan Minuman", "Transportasi", "Hiburan", "Kesehatan", "Lain-lain")

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(requireContext(), "Izin kamera diperlukan untuk menggunakan fitur ini.", Toast.LENGTH_SHORT).show()
        }
    }

    private val captureImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            binding.textScanResult.text = getString(R.string.foto_diambil)
        } else {
            Toast.makeText(requireContext(), "Tidak ada foto yang diambil.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTambahBinding.inflate(inflater, container, false)

        val bottomNavigationView = requireActivity().findViewById<View>(R.id.nav_view)
        bottomNavigationView.visibility = View.GONE

        setupAmountFormatter()

        binding.iconCamera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                openCamera()
            } else {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

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

        var isPengeluaran = binding.toggleGroup.checkedRadioButtonId == R.id.radio_pengeluaran
        setupCategorySpinner(if (isPengeluaran) pengeluaranCategory else pemasukanCategory)

        toggleAdditionalFieldsVisibility(isPengeluaran)

        binding.toggleGroup.setOnCheckedChangeListener { _, checkedId ->
            lifecycleScope.launch {
                val checkedIsPengeluaran = checkedId == R.id.radio_pengeluaran
                val categories = if (checkedIsPengeluaran) pengeluaranCategory else pemasukanCategory

                withContext(Dispatchers.Main) {
                    isPengeluaran = checkedIsPengeluaran
                    setupCategorySpinner(categories)
                    toggleAdditionalFieldsVisibility(checkedIsPengeluaran)
                }
            }
        }

        binding.buttonCreate.setOnClickListener {
            lifecycleScope.launch {
                val kategori = binding.spinnerCategory.selectedItem.toString()
                val nominalFormatted = binding.inputAmount.text.toString()
                val nominal = nominalFormatted.replace("[,.]".toRegex(), "").toLong().toString()
                val deskripsi = binding.inputDescription.text.toString()
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

                catatanViewModel.addCatatan(selectedDate!!, kategori, adjustedNominal, deskripsi)

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Catatan berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                    bottomNavigationView.visibility = View.VISIBLE
                    findNavController().navigateUp()
                }
            }
        }

        return binding.root
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            captureImageLauncher.launch(intent)
        } else {
            Toast.makeText(requireContext(), "Kamera tidak tersedia.", Toast.LENGTH_SHORT).show()
        }
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

    private fun toggleAdditionalFieldsVisibility(isPengeluaran: Boolean) {
        binding.iconCamera.visibility = if (isPengeluaran) View.VISIBLE else View.GONE
        binding.titleScanResult.visibility = if (isPengeluaran) View.VISIBLE else View.GONE
        binding.textScanResult.visibility = if (isPengeluaran) View.VISIBLE else View.GONE
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

    private fun setupAmountFormatter() {
        binding.inputAmount.addTextChangedListener(object : TextWatcher {
            private var currentText = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.toString() != currentText) {
                    binding.inputAmount.removeTextChangedListener(this)

                    val cleanString = s.toString().replace("[,.]".toRegex(), "")
                    if (cleanString.isNotEmpty()) {
                        val formatted = NumberFormat.getNumberInstance(Locale("in", "ID"))
                            .format(cleanString.toDouble())
                        currentText = formatted
                        binding.inputAmount.setText(formatted)
                        binding.inputAmount.setSelection(formatted.length)
                    }

                    binding.inputAmount.addTextChangedListener(this)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
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

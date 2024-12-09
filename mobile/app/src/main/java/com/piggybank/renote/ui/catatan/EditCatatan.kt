package com.piggybank.renotes.ui.catatan

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.piggybank.renote.data.response.HapusCatatanResponse
import com.piggybank.renotes.R
import com.piggybank.renotes.data.pref.UserPreference
import com.piggybank.renotes.data.retrofit.ApiConfig
import com.piggybank.renotes.databinding.FragmentEditCatatanBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class EditCatatan : Fragment() {

    private var _binding: FragmentEditCatatanBinding? = null
    private val binding get() = _binding!!
    private val catatanViewModel: CatatanViewModel by activityViewModels()

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditCatatanBinding.inflate(inflater, container, false)

        val bottomNavigationView = requireActivity().findViewById<View>(R.id.nav_view)
        bottomNavigationView.visibility = View.GONE

        val selectedCatatan = catatanViewModel.selectedCatatan
        if (selectedCatatan != null) {
            binding.inputAmount.setText(formatCurrency(selectedCatatan.nominal))
            binding.inputDescription.setText(selectedCatatan.deskripsi)
        } else {
            lifecycleScope.launch {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Tidak ada catatan yang dipilih!", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            }
        }

        setupAmountFormatter()

        binding.buttonEdit.setOnClickListener {
            lifecycleScope.launch {
                val newNominalFormatted = binding.inputAmount.text.toString()
                val newNominal = newNominalFormatted.replace("[,.]".toRegex(), "").toIntOrNull()
                val newDeskripsi = binding.inputDescription.text.toString()

                if (newNominal != null && newDeskripsi.isNotBlank() && selectedCatatan != null) {
                    catatanViewModel.editCatatan(newNominal, newDeskripsi)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Catatan berhasil diubah!", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(),
                            "Nominal atau deskripsi tidak valid! Pastikan input benar.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        binding.deleteIcon.setOnClickListener {
            lifecycleScope.launch {
                val token = UserPreference(requireContext()).getToken()
                val client = ApiConfig.getApiService(token ?: "")
                val noteId = selectedCatatan!!.id

                val date = Calendar.getInstance().apply {
                    val parts = selectedCatatan.tanggal.split("-")
                    set(parts[2].toInt(), parts[1].toInt() - 1, parts[0].toInt())
                }
                catatanViewModel.deleteSelectedCatatan(date)

                // Hapus dari server
                withContext(Dispatchers.IO) {
                    client.deleteNote(noteId).enqueue(object :
                        Callback<HapusCatatanResponse> {
                        override fun onResponse(
                            call: Call<HapusCatatanResponse>,
                            response: Response<HapusCatatanResponse>
                        ) {
                            if (response.isSuccessful) {
                                lifecycleScope.launch(Dispatchers.Main) {
                                    Toast.makeText(requireContext(), "Catatan berhasil dihapus dari server!", Toast.LENGTH_SHORT).show()
                                    findNavController().navigateUp()
                                }
                            } else {
                                lifecycleScope.launch(Dispatchers.Main) {
                                    Toast.makeText(requireContext(), "Gagal menghapus catatan dari server!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                        override fun onFailure(call: Call<HapusCatatanResponse>, t: Throwable) {
                            lifecycleScope.launch(Dispatchers.Main) {
                                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    })
                }
            }
        }

        binding.topBar.setOnClickListener {
            lifecycleScope.launch {
                withContext(Dispatchers.Main) {
                    findNavController().navigateUp()
                }
            }
        }

        return binding.root
    }

    private fun setupAmountFormatter() {
        binding.inputAmount.addTextChangedListener(object : TextWatcher {
            private var currentText = ""
            private var isNegative = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.toString() != currentText) {
                    binding.inputAmount.removeTextChangedListener(this)

                    val cleanString = s.toString().replace("[^\\d-]".toRegex(), "")
                    isNegative = cleanString.startsWith("-")

                    val sanitizedString = if (isNegative) {
                        "-" + cleanString.replace("-", "")
                    } else {
                        cleanString.replace("-", "")
                    }

                    try {
                        val rawAmount = sanitizedString.replace("-", "").toIntOrNull() ?: 0
                        val formatted = formatCurrency(rawAmount)
                        currentText = if (isNegative) "-$formatted" else formatted
                        binding.inputAmount.setText(currentText)
                        binding.inputAmount.setSelection(currentText.length)
                    } catch (e: NumberFormatException) {
                        currentText = if (isNegative) "-" else ""
                        binding.inputAmount.setText(currentText)
                        binding.inputAmount.setSelection(currentText.length)
                    }

                    binding.inputAmount.addTextChangedListener(this)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }


    private fun formatCurrency(value: Int): String {
        return NumberFormat.getNumberInstance(Locale("in", "ID")).format(value)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStop() {
        super.onStop()
        val bottomNavigationView = requireActivity().findViewById<View>(R.id.nav_view)
        bottomNavigationView.visibility = View.VISIBLE
    }
}

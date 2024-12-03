package com.piggybank.renote.ui.catatan

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
import com.piggybank.renote.R
import com.piggybank.renote.databinding.FragmentEditCatatanBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
            binding.inputAmount.setText(formatCurrency(selectedCatatan.nominal.toString()))
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
                val newNominal = newNominalFormatted.replace("[,.]".toRegex(), "").toLongOrNull()
                val newDeskripsi = binding.inputDescription.text.toString()

                if (newNominal != null && newDeskripsi.isNotBlank() && selectedCatatan != null) {
                    catatanViewModel.editCatatan(newNominal.toString(), newDeskripsi)
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
                val dateKey = selectedCatatan!!.tanggal.split("-")
                val date = Calendar.getInstance().apply {
                    set(dateKey[0].toInt(), dateKey[1].toInt() - 1, dateKey[2].toInt())
                }
                catatanViewModel.deleteSelectedCatatan(date)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Catatan berhasil dihapus!", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
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

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.toString() != currentText) {
                    binding.inputAmount.removeTextChangedListener(this)

                    val cleanString = s.toString().replace("[,.]".toRegex(), "")
                    if (cleanString.isNotEmpty() && cleanString != "-") {
                        try {
                            val formatted = NumberFormat.getNumberInstance(Locale("in", "ID"))
                                .format(cleanString.toDouble())
                            currentText = formatted
                            binding.inputAmount.setText(formatted)
                            binding.inputAmount.setSelection(formatted.length)
                        } catch (e: NumberFormatException) {
                            currentText = ""
                            binding.inputAmount.setText("")
                        }
                    } else {
                        currentText = s.toString()
                    }

                    binding.inputAmount.addTextChangedListener(this)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }


    private fun formatCurrency(value: String): String {
        return NumberFormat.getNumberInstance(Locale("in", "ID")).format(value.toLong())
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

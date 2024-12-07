package com.piggybank.renotes.ui.rekening

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.piggybank.renotes.R
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class TambahRekening : Fragment(R.layout.fragment_tambah_rekening) {

    private lateinit var rekeningViewModel: RekeningViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rekeningViewModel = ViewModelProvider(requireActivity())[RekeningViewModel::class.java]

        val inputNamaRekening = view.findViewById<EditText>(R.id.input_nama_rekening)
        val inputJumlahRekening = view.findViewById<EditText>(R.id.input_jumlah_rekening)

        inputJumlahRekening.addTextChangedListener(object : TextWatcher {
            private var isEditing = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isEditing) return

                isEditing = true
                val rawValue = s.toString().replace(".", "").toIntOrNull() ?: 0
                inputJumlahRekening.setText(formatCurrency(rawValue))
                inputJumlahRekening.setSelection(inputJumlahRekening.text.length)
                isEditing = false
            }
        })

        view.findViewById<View>(R.id.icon_back).setOnClickListener {
            findNavController().navigateUp()
        }

        view.findViewById<Button>(R.id.button_simpan).setOnClickListener {
            val namaRekening = inputNamaRekening.text.toString()
            val jumlahRekening = inputJumlahRekening.text.toString().replace(".", "")

            if (namaRekening.isNotBlank() && jumlahRekening.isNotBlank()) {
                val saldo = jumlahRekening.toIntOrNull()
                if (saldo != null) {
                    val rekeningBaru = Rekening(namaRekening, saldo)
                    lifecycleScope.launch {
                        val isAdded = rekeningViewModel.addRekening(rekeningBaru)
                        if (isAdded) {
                            findNavController().navigateUp()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Rekening '$namaRekening' Sudah Terdaftar",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Jumlah rekening tidak valid", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Harap isi semua untuk melengkapi data Rekening", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().findViewById<View>(R.id.nav_view)?.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        requireActivity().findViewById<View>(R.id.nav_view)?.visibility = View.VISIBLE
    }

    private fun formatCurrency(value: Int): String {
        return NumberFormat.getNumberInstance(Locale("id", "ID")).format(value)
    }
}

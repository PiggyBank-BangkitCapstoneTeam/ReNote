package com.piggybank.renote.ui.rekening

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.piggybank.renote.R

class TambahRekening : Fragment(R.layout.fragment_tambah_rekening) {

    private lateinit var rekeningViewModel: RekeningViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rekeningViewModel = ViewModelProvider(requireActivity()).get(RekeningViewModel::class.java)

        view.findViewById<View>(R.id.icon_back).setOnClickListener {
            findNavController().navigateUp()
        }

        view.findViewById<Button>(R.id.button_simpan).setOnClickListener {
            val namaRekening = view.findViewById<EditText>(R.id.input_nama_rekening).text.toString()
            val jumlahRekening = view.findViewById<EditText>(R.id.input_jumlah_rekening).text.toString()

            if (namaRekening.isNotBlank() && jumlahRekening.isNotBlank()) {
                val saldo = jumlahRekening.toDoubleOrNull()
                if (saldo != null) {
                    val rekeningBaru = Rekening(namaRekening, saldo.toLong())
                    // Cek apakah rekening sudah ada
                    val isAdded = rekeningViewModel.addRekening(rekeningBaru)
                    if (isAdded) {
                        findNavController().navigateUp()
                    } else {
                        Toast.makeText(requireContext(), "Rekening Sudah '$namaRekening' Terdaftar", Toast.LENGTH_SHORT).show()
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
}
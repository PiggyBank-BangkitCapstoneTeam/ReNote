package com.piggybank.renote.ui.catatan

import android.os.Bundle
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
import java.util.Calendar

class EditCatatan : Fragment() {

    private var _binding: FragmentEditCatatanBinding? = null
    private val binding get() = _binding!!
    private val catatanViewModel: CatatanViewModel by activityViewModels()

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
            binding.inputAmount.setText(selectedCatatan.nominal)
            binding.inputDescription.setText(selectedCatatan.deskripsi)
        } else {
            Toast.makeText(requireContext(), "Tidak ada catatan yang dipilih!", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }

        binding.buttonEdit.setOnClickListener {
            val newNominal = binding.inputAmount.text.toString()
            val newDeskripsi = binding.inputDescription.text.toString()

            if (newNominal.isNotBlank() && newDeskripsi.isNotBlank() && selectedCatatan != null) {
                val dateKey = selectedCatatan.tanggal.split("-")
                val date = Calendar.getInstance().apply {
                    set(dateKey[2].toInt(), dateKey[1].toInt() - 1, dateKey[0].toInt())
                }

                lifecycleScope.launch(Dispatchers.IO) {
                    catatanViewModel.editCatatan(date, newNominal, newDeskripsi)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Catatan berhasil diubah!", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Nominal atau deskripsi tidak valid!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.deleteIcon.setOnClickListener {
            val dateKey = selectedCatatan!!.tanggal.split("-")
            val date = Calendar.getInstance().apply {
                set(dateKey[2].toInt(), dateKey[1].toInt() - 1, dateKey[0].toInt())
            }
            lifecycleScope.launch(Dispatchers.IO) {
                catatanViewModel.deleteSelectedCatatan(date)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Catatan berhasil dihapus!", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            }
        }

        binding.topBar.setOnClickListener {
            findNavController().navigateUp()
        }

        return binding.root
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

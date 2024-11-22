package com.piggybank.renote.ui.rekening

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.piggybank.renote.databinding.FragmentEditRekeningBinding

class EditRekeningFragment : Fragment() {

    private var _binding: FragmentEditRekeningBinding? = null
    private val binding get() = _binding!!

    private lateinit var rekeningViewModel: RekeningViewModel
    private val args: EditRekeningFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        rekeningViewModel = ViewModelProvider(requireActivity()).get(RekeningViewModel::class.java)

        _binding = FragmentEditRekeningBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.rekeningNameEdit.setText(args.rekening.name)
        binding.rekeningJumlahEdit.setText(args.rekening.uang.toString())

        binding.buttonSimpan.setOnClickListener {
            val newAmount = binding.rekeningJumlahEdit.text.toString()

            if (TextUtils.isEmpty(newAmount)) {
                Toast.makeText(context, "Jumlah rekening tidak boleh kosong", Toast.LENGTH_SHORT).show()
            } else {
                val updatedRekening = args.rekening.copy(uang = newAmount.toLong())

                val isUpdated = rekeningViewModel.updateRekening(updatedRekening)

                if (isUpdated) {
                    findNavController().navigateUp()
                } else {
                    Toast.makeText(context, "Terjadi kesalahan saat memperbarui rekening", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.iconBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.deleteIcon.setOnClickListener {
            val isDeleted = rekeningViewModel.deleteRekening(args.rekening)

            if (isDeleted) {
                Toast.makeText(context, "Rekening berhasil dihapus", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            } else {
                Toast.makeText(context, "Terjadi kesalahan saat menghapus rekening", Toast.LENGTH_SHORT).show()
            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

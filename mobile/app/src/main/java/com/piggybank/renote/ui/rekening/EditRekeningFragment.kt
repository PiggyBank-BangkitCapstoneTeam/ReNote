package com.piggybank.renote.ui.rekening

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.piggybank.renote.databinding.FragmentEditRekeningBinding
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class EditRekeningFragment : Fragment() {

    private var _binding: FragmentEditRekeningBinding? = null
    private val binding get() = _binding!!

    private lateinit var rekeningViewModel: RekeningViewModel
    private val args: EditRekeningFragmentArgs by navArgs()

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        rekeningViewModel = ViewModelProvider(requireActivity())[RekeningViewModel::class.java]

        _binding = FragmentEditRekeningBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.rekeningNameEdit.setText(args.rekening.name)
        binding.rekeningJumlahEdit.setText(formatCurrency(args.rekening.uang))

        binding.rekeningJumlahEdit.addTextChangedListener(object : TextWatcher {
            private var isEditing = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isEditing) return

                isEditing = true
                val rawValue = s.toString().replace(".", "").toIntOrNull() ?: 0
                binding.rekeningJumlahEdit.setText(formatCurrency(rawValue))
                binding.rekeningJumlahEdit.setSelection(binding.rekeningJumlahEdit.text.length)
                isEditing = false
            }
        })

        binding.buttonSimpan.setOnClickListener {
            val newAmount = binding.rekeningJumlahEdit.text.toString().replace(".", "")

            if (TextUtils.isEmpty(newAmount)) {
                Toast.makeText(context, "Jumlah rekening tidak boleh kosong", Toast.LENGTH_SHORT).show()
            } else {
                lifecycleScope.launch {
                    val updatedRekening = args.rekening.copy(uang = newAmount.toInt())

                    val isUpdated = rekeningViewModel.updateRekening(updatedRekening)

                    if (isUpdated) {
                        findNavController().navigateUp()
                    } else {
                        Toast.makeText(context, "Terjadi kesalahan saat memperbarui rekening", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.iconBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.deleteIcon.setOnClickListener {
            lifecycleScope.launch {
                val isDeleted = rekeningViewModel.deleteRekening(args.rekening)

                if (isDeleted) {
                    Toast.makeText(context, "Rekening berhasil dihapus", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                } else {
                    Toast.makeText(context, "Terjadi kesalahan saat menghapus rekening", Toast.LENGTH_SHORT).show()
                }
            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun formatCurrency(value: Int): String {
        return NumberFormat.getNumberInstance(Locale("id", "ID")).format(value)
    }
}

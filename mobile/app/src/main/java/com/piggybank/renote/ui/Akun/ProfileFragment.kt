package com.piggybank.renote.ui.Akun

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.piggybank.renote.R
import com.piggybank.renote.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedPref: SharedPreferences

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    try {
                        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
                        cursor?.use {
                            val sizeIndex = it.getColumnIndex(MediaStore.Images.Media.SIZE)
                            it.moveToFirst()
                            val fileSize = it.getLong(sizeIndex)
                            if (fileSize <= 2 * 1024 * 1024) {
                                binding.fotoProfile.setImageURI(uri)
                                saveImageToSharedPreferences(uri)
                            } else {
                                Toast.makeText(requireContext(), "Ukuran file melebihi 2MB", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(requireContext(), "Gagal memuat gambar", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        sharedPref = requireActivity().getSharedPreferences("UserData", Activity.MODE_PRIVATE)
        return binding.root
    }

    @SuppressLint("IntentReset")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bottomNavigationView = activity?.findViewById<View>(R.id.nav_view)
        bottomNavigationView?.visibility = View.GONE

        loadUserData()

        binding.backButton.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.addIcon.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type = "image/*"
            val mimeTypes = arrayOf("image/jpeg", "image/png", "image/jpg")
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            pickImage.launch(intent)
        }

        binding.btnSimpan.setOnClickListener {
            val inputNama = binding.inputNama.text.toString()
            if (inputNama.isNotBlank()) {
                saveNameToSharedPreferences(inputNama)
                Toast.makeText(requireContext(), "Nama berhasil disimpan!", Toast.LENGTH_SHORT).show()
                requireActivity().onBackPressedDispatcher.onBackPressed()
            } else {
                Toast.makeText(requireContext(), "Nama tidak boleh kosong!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveImageToSharedPreferences(uri: Uri) {
        with(sharedPref.edit()) {
            putString("userImage", uri.toString())
            apply()
        }
    }

    private fun saveNameToSharedPreferences(name: String) {
        with(sharedPref.edit()) {
            putString("userName", name)
            apply()
        }
    }

    private fun loadUserData() {
        val userName = sharedPref.getString("userName", "")
        val userImageUri = sharedPref.getString("userImage", null)

        if (!userName.isNullOrBlank()) {
            binding.inputNama.setText(userName)
        }

        if (!userImageUri.isNullOrEmpty()) {
            try {
                val uri = Uri.parse(userImageUri)
                binding.fotoProfile.setImageURI(uri)
            } catch (e: Exception) {
                e.printStackTrace()
                binding.fotoProfile.setImageResource(R.drawable.profile)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val bottomNavigationView = activity?.findViewById<View>(R.id.nav_view)
        bottomNavigationView?.visibility = View.VISIBLE
        _binding = null
    }
}

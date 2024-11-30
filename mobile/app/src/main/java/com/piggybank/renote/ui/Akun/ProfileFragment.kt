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
import java.io.File
import java.io.FileOutputStream

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedPref: SharedPreferences
    private var temporaryImagePath: String? = null

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    updateImageView(uri)
                    temporaryImagePath = saveImageToInternalStorage(uri)
                    if (temporaryImagePath.isNullOrEmpty()) {
                        Toast.makeText(requireContext(), "Gagal menyimpan gambar", Toast.LENGTH_SHORT).show()
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

                // Simpan gambar baru ke SharedPreferences
                temporaryImagePath?.let { saveImageToSharedPreferences(it) }
                Toast.makeText(requireContext(), "Data berhasil disimpan!", Toast.LENGTH_SHORT).show()

                requireActivity().onBackPressedDispatcher.onBackPressed()
            } else {
                Toast.makeText(requireContext(), "Nama tidak boleh kosong!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteOldProfileImage() {
        val oldImagePath = sharedPref.getString("userImage", null)
        if (!oldImagePath.isNullOrEmpty() && oldImagePath != temporaryImagePath) {
            val oldFile = File(oldImagePath)
            if (oldFile.exists()) {
                oldFile.delete()
            }
        }
    }

    private fun saveImageToInternalStorage(uri: Uri): String? {
        return try {
            deleteOldProfileImage()

            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val file = File(requireContext().filesDir, "profile_image_temp.jpg") // Simpan gambar sementara
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()

            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveImageToSharedPreferences(filePath: String) {
        with(sharedPref.edit()) {
            putString("userImage", filePath)
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
        val userImagePath = sharedPref.getString("userImage", null)

        if (!userName.isNullOrBlank()) {
            binding.inputNama.setText(userName)
        }

        if (!userImagePath.isNullOrEmpty()) {
            updateImageView(Uri.fromFile(File(userImagePath)))
        }
    }

    private fun updateImageView(uri: Uri) {
        binding.fotoProfile.setImageURI(uri)
        binding.fotoProfile.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val bottomNavigationView = activity?.findViewById<View>(R.id.nav_view)
        bottomNavigationView?.visibility = View.VISIBLE
        _binding = null
    }
}

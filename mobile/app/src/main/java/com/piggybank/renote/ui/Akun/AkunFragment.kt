package com.piggybank.renote.ui.Akun

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.piggybank.renote.R
import com.piggybank.renote.databinding.FragmentAkunBinding

class AkunFragment : Fragment() {

    private var _binding: FragmentAkunBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedPref: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAkunBinding.inflate(inflater, container, false)
        sharedPref = requireActivity().getSharedPreferences("UserData", Activity.MODE_PRIVATE)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateUserName()
        updateUserImage()

        binding.menuProfile.setOnClickListener {
            findNavController().navigate(R.id.action_akunFragment_to_profileFragment)
        }

        binding.menuBantuan.setOnClickListener {
            findNavController().navigate(R.id.action_akunFragment_to_bantuanFragment)
        }

        binding.menuTentang.setOnClickListener {
            findNavController().navigate(R.id.action_akunFragment_to_tentangFragment)
        }

        binding.menuBahasa.setOnClickListener {
            val intent = Intent(Settings.ACTION_LOCALE_SETTINGS)
            startActivity(intent)
        }
    }

    private fun updateUserName() {
        val userName = sharedPref.getString("userName", getString(R.string.nama))
        binding.userName.text = userName ?: getString(R.string.nama)
    }

    private fun updateUserImage() {
        val userImageUri = sharedPref.getString("userImage", null)
        if (!userImageUri.isNullOrEmpty()) {
            try {
                val uri = Uri.parse(userImageUri)
                binding.userImage.setImageURI(uri)
            } catch (e: Exception) {
                e.printStackTrace()
                binding.userImage.setImageResource(R.drawable.profile)
            }
        } else {
            binding.userImage.setImageResource(R.drawable.profile)
        }
    }

    override fun onResume() {
        super.onResume()
        updateUserName()
        updateUserImage()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

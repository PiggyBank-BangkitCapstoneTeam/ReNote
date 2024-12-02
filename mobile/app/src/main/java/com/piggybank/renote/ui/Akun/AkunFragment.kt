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
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.piggybank.renote.R
import com.piggybank.renote.databinding.FragmentAkunBinding
import java.io.File

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
        val userImagePath = sharedPref.getString("userImage", null)
        if (!userImagePath.isNullOrEmpty()) {
            val file = File(userImagePath)
            if (file.exists()) {
                binding.userImage.apply {
                    setImageURI(Uri.fromFile(file))
                    scaleType = ImageView.ScaleType.CENTER_CROP
                }
            } else {
                binding.userImage.apply {
                    setImageResource(R.drawable.profile)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                }
            }
        } else {
            binding.userImage.apply {
                setImageResource(R.drawable.profile)
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
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
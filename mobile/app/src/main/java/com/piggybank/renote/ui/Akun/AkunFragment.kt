package com.piggybank.renote.ui.Akun

import android.content.Intent
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAkunBinding.inflate(inflater, container, false)
        val root: View = binding.root

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

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

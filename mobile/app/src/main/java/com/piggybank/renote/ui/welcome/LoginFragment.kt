package com.piggybank.renote.ui.welcome

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import com.piggybank.renote.R
import com.piggybank.renote.databinding.FragmentLoginBinding
import com.piggybank.renote.ui.main.MainActivity

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val enterAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.enter_animation)
        binding.root.startAnimation(enterAnimation)

        binding.gridSlider.setOnClickListener {
            navigateToMainActivity()
        }

        binding.logoCard.setOnClickListener {
            navigateToMainActivity()
        }
    }

    private fun navigateToMainActivity() {
        val exitAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.exit_animation)
        binding.root.startAnimation(exitAnimation)

        val intent = Intent(requireContext(), MainActivity::class.java)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

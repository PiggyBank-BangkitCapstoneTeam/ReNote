package com.piggybank.renote.ui.welcome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import com.piggybank.renote.R
import com.piggybank.renote.databinding.FragmentSecondBinding

class SecondFragment : Fragment() {
    private var _binding: FragmentSecondBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val enterAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.enter_animation)
        binding.root.startAnimation(enterAnimation)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        val exitAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.exit_animation)
        binding.root.startAnimation(exitAnimation)

        _binding = null
    }
}

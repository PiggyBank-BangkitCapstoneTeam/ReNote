package com.piggybank.renote.ui.welcome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import com.piggybank.renote.R
import com.piggybank.renote.databinding.FragmentFirstBinding // Import the generated binding class

class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val enterAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.enter_animation)
        val exitAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.exit_animation)

        binding.imageIntro.startAnimation(enterAnimation)
        binding.textWelcome.startAnimation(enterAnimation)
        binding.textDescWelcome.startAnimation(enterAnimation)
        binding.gridSlider.startAnimation(enterAnimation)

        requireActivity().supportFragmentManager.addOnBackStackChangedListener {
            if (!requireActivity().supportFragmentManager.fragments.contains(this)) {
                binding.root.startAnimation(exitAnimation)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

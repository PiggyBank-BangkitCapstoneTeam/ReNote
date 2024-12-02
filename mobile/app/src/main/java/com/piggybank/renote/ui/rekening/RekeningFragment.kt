package com.piggybank.renote.ui.rekening

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.piggybank.renote.R
import com.piggybank.renote.databinding.FragmentRekeningBinding

class RekeningFragment : Fragment() {
    private var _binding: FragmentRekeningBinding? = null
    private val binding get() = _binding!!

    private lateinit var rekeningViewModel: RekeningViewModel
    private lateinit var userId: String
    private lateinit var adapter: RekeningAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        rekeningViewModel = ViewModelProvider(requireActivity())[RekeningViewModel::class.java]
        _binding = FragmentRekeningBinding.inflate(inflater, container, false)

        val currentUser = FirebaseAuth.getInstance().currentUser
        userId = currentUser?.uid ?: ""

        rekeningViewModel.setUserId(userId)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rekeningViewModel.rekeningList.observe(viewLifecycleOwner) { rekeningList ->
            adapter = RekeningAdapter(
                rekeningList,
                { rekening ->
                    rekeningViewModel.setActiveRekening(rekening)
                    val action = RekeningFragmentDirections.actionRekeningFragmentToEditRekening(rekening)
                    findNavController().navigate(action)
                },
                rekeningViewModel::formatCurrency
            )
            binding.rekeningList.layoutManager = LinearLayoutManager(requireContext())
            binding.rekeningList.adapter = adapter
        }

        rekeningViewModel.totalSaldo.observe(viewLifecycleOwner) { totalSaldo ->
            binding.totalSaldo.text = rekeningViewModel.formatCurrency(totalSaldo)
        }

        binding.rekeningAdd.setOnClickListener {
            findNavController().navigate(R.id.action_rekeningFragment_to_tambahRekening)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


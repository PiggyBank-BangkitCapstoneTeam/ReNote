package com.piggybank.renote.ui.Akun

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.piggybank.renote.R

class BantuanFragment : Fragment() {

    private lateinit var messageAdapter: MessageAdapter
    private val messages = mutableListOf<Message>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bantuan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bottomNavigationView = activity?.findViewById<View>(R.id.nav_view)
        bottomNavigationView?.visibility = View.GONE

        val recyclerView = view.findViewById<RecyclerView>(R.id.messageRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        messageAdapter = MessageAdapter(messages)
        recyclerView.adapter = messageAdapter

        val backButton = view.findViewById<View>(R.id.backButton)
        backButton.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    requireActivity().supportFragmentManager.popBackStack()
                }
            }
        )

        loadMessages()
    }

    private fun loadMessages() {
        messages.add(Message("Admin", "Selamat datang di bantuan!", "13/12/2024"))
        messages.add(Message("User", "Saya butuh bantuan dengan aplikasi.", "13/12/2024"))
        messages.add(Message("Admin", "Tentu, bagaimana kami bisa membantu Anda?", "13/12/2024"))

        messageAdapter.notifyItemInserted(messages.size - 1)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val bottomNavigationView = activity?.findViewById<View>(R.id.nav_view)
        bottomNavigationView?.visibility = View.VISIBLE
    }
}

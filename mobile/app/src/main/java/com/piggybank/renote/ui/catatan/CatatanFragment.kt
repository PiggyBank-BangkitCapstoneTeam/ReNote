package com.piggybank.renotes.ui.catatan

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.piggybank.renote.data.response.GetAllNoteResponse
import com.piggybank.renotes.R
import com.piggybank.renotes.data.pref.UserPreference
import com.piggybank.renotes.data.retrofit.ApiConfig
import com.piggybank.renotes.databinding.FragmentCatatanBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CatatanFragment : Fragment() {

    private var selectedDate: Calendar = Calendar.getInstance()
    private var _binding: FragmentCatatanBinding? = null
    private val binding get() = _binding!!

    private lateinit var catatanAdapter: CatatanAdapter
    private lateinit var firebaseAuth: FirebaseAuth
    private val catatanViewModel: CatatanViewModel by activityViewModels()
    private lateinit var userPreference: UserPreference

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCatatanBinding.inflate(inflater, container, false)
        userPreference = UserPreference(requireContext())
        setupRecyclerView()
        firebaseAuth = FirebaseAuth.getInstance()
        fetchNotesFromApi()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.catatanAdd.setOnClickListener {
            lifecycleScope.launch {
                catatanViewModel.clearSelectedCatatan()
                withContext(Dispatchers.Main) {
                    findNavController().navigate(R.id.navigation_tambahCatatan)
                }
            }
        }

        binding.calendarButton.setOnClickListener {
            showDatePickerDialog()
        }

        updateUIForDate(selectedDate)
    }

    private fun setupRecyclerView() {
        catatanAdapter = CatatanAdapter { catatan ->
            lifecycleScope.launch {
                catatanViewModel.selectedCatatan = catatan
                withContext(Dispatchers.Main) {
                    findNavController().navigate(R.id.navigation_editCatatan)
                }
            }
        }

        binding.transactionRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = catatanAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    private fun fetchNotesFromApi() {
        val token = userPreference.getToken()
        val userId = firebaseAuth.currentUser?.uid

        if (token != null && userId != null) {
            val client = ApiConfig.getApiService(token)
            client.getAllNotes().enqueue(object : Callback<GetAllNoteResponse> {
                override fun onResponse(
                    call: Call<GetAllNoteResponse>,
                    response: Response<GetAllNoteResponse>
                ) {
                    if (response.isSuccessful) {
                        val selectedDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            .format(selectedDate.time)

                        val notes = response.body()?.data?.mapNotNull {
                            it?.let { dataItem ->
                                Catatan(
                                    kategori = dataItem.kategori ?: "",
                                    nominal = dataItem.nominal ?: 0,
                                    deskripsi = dataItem.deskripsi ?: "",
                                    tanggal = dataItem.tanggal ?: ""
                                )
                            }
                        }?.filter {
                            it.tanggal == selectedDateString
                        } ?: emptyList()

                        catatanAdapter.submitList(notes)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Failed to fetch notes",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<GetAllNoteResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            })
        } else {
            Toast.makeText(
                requireContext(),
                "No token or user ID found, please log in",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    private fun updateUIForDate(date: Calendar) {
        val currentUser = userPreference.getToken()
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        catatanViewModel.setUserId(currentUser)

        catatanViewModel.updateDataForDate(date)

        catatanViewModel.catatanList.observe(viewLifecycleOwner) { catatanList ->
            lifecycleScope.launch {
                withContext(Dispatchers.Main) {
                    catatanAdapter.submitList(catatanList)
                }
            }
        }

        catatanViewModel.totalPemasukan.observe(viewLifecycleOwner) { pemasukan ->
            lifecycleScope.launch {
                val formattedPemasukan =
                    NumberFormat.getNumberInstance(Locale.getDefault()).format(pemasukan)
                withContext(Dispatchers.Main) {
                    binding.textPemasukan.text =
                        getString(R.string.pemasukan_text, formattedPemasukan)
                }
            }
        }

        catatanViewModel.totalPengeluaran.observe(viewLifecycleOwner) { pengeluaran ->
            lifecycleScope.launch {
                val formattedPengeluaran =
                    NumberFormat.getNumberInstance(Locale.getDefault()).format(pengeluaran)
                withContext(Dispatchers.Main) {
                    binding.textPengeluaran.text =
                        getString(R.string.pengeluaran_text, formattedPengeluaran)
                }
            }
        }
    }


    private fun showDatePickerDialog() {
        val year = selectedDate.get(Calendar.YEAR)
        val month = selectedDate.get(Calendar.MONTH)
        val day = selectedDate.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                selectedDate = Calendar.getInstance().apply {
                    set(selectedYear, selectedMonth, selectedDay)
                }
                updateUIForDate(selectedDate)
            },
            year, month, day
        )

        datePickerDialog.show()
    }

    override fun onResume() {
        super.onResume()
        updateUIForDate(selectedDate)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

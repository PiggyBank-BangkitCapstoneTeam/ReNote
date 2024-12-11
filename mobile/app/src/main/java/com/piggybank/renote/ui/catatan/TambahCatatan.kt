package com.piggybank.renotes.ui.catatan

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.piggybank.renote.data.response.TambahCatatanResponse
import com.piggybank.renotes.data.retrofit.ApiConfig
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.piggybank.renotes.R
import com.piggybank.renotes.data.pref.UserPreference
import com.piggybank.renotes.databinding.FragmentTambahBinding
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TambahCatatan : Fragment() {

    private var _binding: FragmentTambahBinding? = null
    private val binding get() = _binding!!

    private val catatanViewModel: CatatanViewModel by activityViewModels()

    private var selectedDate: Calendar? = null
    private var currentNoteId: String? = null
    private var uploadedImageUrl: String? = null

    private val pemasukanCategory = listOf("Pilih Kategori", "Gaji", "Investasi", "Paruh Waktu", "Lain-lain")
    private val pengeluaranCategory = listOf("Pilih Kategori", "Parkir", "Makanan dan Minuman", "Transportasi", "Hiburan", "Kesehatan", "Lain-lain")

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(requireContext(), "Izin kamera diperlukan untuk menggunakan fitur ini.", Toast.LENGTH_SHORT).show()
        }
    }

    private val captureImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            val photoUri = Uri.fromFile(File(requireContext().cacheDir, "temp_image.jpg"))
            val destinationUri = Uri.fromFile(File(requireContext().cacheDir, "cropped_image.jpg"))
            val options = UCrop.Options().apply {
                setCompressionQuality(80)
                setFreeStyleCropEnabled(true)
                setToolbarTitle(getString(R.string.foto_crop))
            }
            UCrop.of(photoUri, destinationUri)
                .withOptions(options)
                .getIntent(requireContext())
                .let { uCropLauncher.launch(it) }
        } else {
            Toast.makeText(requireContext(), "Tidak ada foto yang diambil.", Toast.LENGTH_SHORT).show()
        }
    }

    private val uCropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let {
                val photoFile = File(resultUri.path!!)
                uploadCroppedPhoto(photoFile)
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(result.data!!)
            Toast.makeText(requireContext(), "Gagal mencrop gambar: ${cropError?.message}", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTambahBinding.inflate(inflater, container, false)

        val bottomNavigationView = requireActivity().findViewById<View>(R.id.nav_view)
        bottomNavigationView.visibility = View.GONE

        setupAmountFormatter()

        binding.iconCamera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                openCamera()
            } else {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        binding.iconBack.setOnClickListener {
            lifecycleScope.launch {
                withContext(Dispatchers.Main) {
                    bottomNavigationView.visibility = View.VISIBLE
                    findNavController().navigateUp()
                }
            }
        }

        binding.iconCalendar.setOnClickListener {
            showDatePickerDialog()
        }

        var isPengeluaran = binding.toggleGroup.checkedRadioButtonId == R.id.radio_pengeluaran
        setupCategorySpinner(if (isPengeluaran) pengeluaranCategory else pemasukanCategory)

        toggleAdditionalFieldsVisibility(isPengeluaran)

        binding.toggleGroup.setOnCheckedChangeListener { _, checkedId ->
            lifecycleScope.launch {
                val checkedIsPengeluaran = checkedId == R.id.radio_pengeluaran
                val categories = if (checkedIsPengeluaran) pengeluaranCategory else pemasukanCategory

                withContext(Dispatchers.Main) {
                    isPengeluaran = checkedIsPengeluaran
                    setupCategorySpinner(categories)
                    toggleAdditionalFieldsVisibility(checkedIsPengeluaran)
                }
            }
        }

        binding.buttonCreate.setOnClickListener {
            lifecycleScope.launch {
                val kategori = binding.spinnerCategory.selectedItem.toString()
                var nominal = getRawAmountValue()
                val deskripsi = binding.inputDescription.text.toString()

                if (selectedDate == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Pilih tanggal terlebih dahulu!", Toast.LENGTH_SHORT).show()
                    }
                    fetchNoteId()
                    return@launch
                }

                if (kategori == "Pilih Kategori" || nominal == 0 || deskripsi.isBlank()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Isi semua data dengan benar!", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                if (isPengeluaran) {
                    nominal = -nominal
                }

                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val formattedDate = sdf.format(selectedDate!!.time)

                catatanViewModel.addCatatan(selectedDate!!, kategori, nominal, deskripsi)

                val token = UserPreference(requireContext()).getToken()
                if (token.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Token tidak ditemukan, gagal mengirim data ke server.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val apiService = ApiConfig.getApiService(token)
                val request = Catatan(
                    id = "",
                    kategori = kategori,
                    nominal = nominal,
                    deskripsi = deskripsi,
                    tanggal = formattedDate
                )

                apiService.addNote(request).enqueue(object : Callback<TambahCatatanResponse> {
                    override fun onResponse(
                        call: Call<TambahCatatanResponse>,
                        response: Response<TambahCatatanResponse>
                    ) {
                        if (!isAdded || isDetached) return

                        lifecycleScope.launch(Dispatchers.Main) {
                            if (response.isSuccessful && response.body() != null) {
                                Toast.makeText(requireContext(), "Catatan berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                                bottomNavigationView.visibility = View.VISIBLE
                                findNavController().navigateUp()
                            } else {
                                Toast.makeText(requireContext(), "Gagal menyimpan catatan ke server. Cek data atau koneksi!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    override fun onFailure(call: Call<TambahCatatanResponse>, t: Throwable) {
                        if (!isAdded || isDetached) return

                        lifecycleScope.launch(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Kesalahan jaringan: ${t.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                })

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Catatan berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                    bottomNavigationView.visibility = View.VISIBLE
                    findNavController().navigateUp()
                }
            }
        }
        return binding.root
    }

    private fun fetchNoteId() {
        lifecycleScope.launch {
            val token = UserPreference(requireContext()).getToken()
            if (token.isNullOrEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Token tidak ditemukan, gagal mengirim data ke server.", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val apiService = ApiConfig.getApiService(token)
            val request = Catatan(
                id = "",
                kategori = "Pengeluaran",
                nominal = 0,
                deskripsi = "",
                tanggal = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
            )

            apiService.addNote(request).enqueue(object : Callback<TambahCatatanResponse> {
                override fun onResponse(
                    call: Call<TambahCatatanResponse>,
                    response: Response<TambahCatatanResponse>
                ) {
                    if (!isAdded || isDetached) return

                    lifecycleScope.launch(Dispatchers.Main) {
                        if (response.isSuccessful && response.body() != null) {
                            currentNoteId = response.body()?.id
                            Toast.makeText(requireContext(), "Catatan anda telah dibuat silahkan upload gambar", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "Gagal mendapatkan Note ID. Cek koneksi atau data!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onFailure(call: Call<TambahCatatanResponse>, t: Throwable) {
                    if (!isAdded || isDetached) return

                    lifecycleScope.launch(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Kesalahan jaringan: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }
    }

    private fun openCamera() {
        val photoFile = File(requireContext().cacheDir, "temp_image.jpg")
        val photoUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", photoFile)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        }
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            captureImageLauncher.launch(intent)
        } else {
            Toast.makeText(requireContext(), "Kamera tidak tersedia.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupCategorySpinner(categories: List<String>) {
        lifecycleScope.launch {
            val adapter = withContext(Dispatchers.Default) {
                ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    categories
                ).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }
            }
            withContext(Dispatchers.Main) {
                binding.spinnerCategory.adapter = adapter
            }
        }
    }

    private fun uploadCroppedPhoto(photoFile: File) {
        lifecycleScope.launch {
            val token = UserPreference(requireContext()).getToken()
            if (token.isNullOrEmpty() || currentNoteId.isNullOrEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Masukkan nominal jumlah 0 terlebih dahulu", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val apiService = ApiConfig.getApiService(token)

            val requestBody = photoFile.asRequestBody("image/*".toMediaTypeOrNull())
            val photoPart = MultipartBody.Part.createFormData("foto", photoFile.name, requestBody)

            withContext(Dispatchers.IO) {
                try {
                    val response = apiService.uploadStruk(currentNoteId!!, photoPart).execute()

                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful && response.body()?.data?.url != null) {
                            uploadedImageUrl = response.body()?.data?.url
                            Toast.makeText(requireContext(), "Foto berhasil diunggah!", Toast.LENGTH_SHORT).show()
                            Toast.makeText(requireContext(), "Struk anda sedang diproses mohon tunggu beberapa saat!", Toast.LENGTH_SHORT).show()
                            findNavController().navigateUp()
                        } else {
                            val errorBody = response.errorBody()?.string()
                            Toast.makeText(requireContext(), "Gagal mengunggah foto: ${errorBody ?: "Kesalahan tak diketahui"}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Kesalahan jaringan: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun toggleAdditionalFieldsVisibility(isPengeluaran: Boolean) {
        binding.iconCamera.visibility = if (isPengeluaran) View.VISIBLE else View.GONE
    }

    private fun showDatePickerDialog() {
        val calendar = selectedDate ?: Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                lifecycleScope.launch {
                    selectedDate = Calendar.getInstance().apply {
                        set(selectedYear, selectedMonth, selectedDay)
                    }
                    withContext(Dispatchers.Main) {
                        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                        val formattedDate = sdf.format(selectedDate!!.time)
                        binding.textDate.text = formattedDate
                    }
                }
            },
            year, month, day
        )

        datePickerDialog.show()
    }

    private var rawAmountValue: Int = 0

    private fun setupAmountFormatter() {
        binding.inputAmount.addTextChangedListener(object : TextWatcher {
            private var currentText = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.toString() != currentText) {
                    binding.inputAmount.removeTextChangedListener(this)

                    val cleanString = s.toString().replace("[,.]".toRegex(), "")
                    if (cleanString.isNotEmpty()) {
                        rawAmountValue = cleanString.toIntOrNull() ?: 0
                        val formatted = NumberFormat.getNumberInstance(Locale("in", "ID"))
                            .format(rawAmountValue.toLong())
                        currentText = formatted
                        binding.inputAmount.setText(formatted)
                        binding.inputAmount.setSelection(formatted.length)
                    }

                    binding.inputAmount.addTextChangedListener(this)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun getRawAmountValue(): Int = rawAmountValue

    override fun onDestroyView() {
        super.onDestroyView()
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                val bottomNavigationView = requireActivity().findViewById<View>(R.id.nav_view)
                bottomNavigationView.visibility = View.VISIBLE
                _binding = null
            }
        }
    }
}
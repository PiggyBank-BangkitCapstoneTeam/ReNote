package com.piggybank.renotes.ui.welcome

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.piggybank.renotes.R
import com.piggybank.renotes.data.response.LoginResponse
import com.piggybank.renotes.data.retrofit.ApiConfig
import com.piggybank.renotes.databinding.FragmentLoginBinding
import com.piggybank.renotes.ui.NetworkUtils
import com.piggybank.renotes.ui.main.MainActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Suppress("DEPRECATION")
class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var signInClient: SignInClient

    private val signInLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                result.data?.let { intent ->
                    val credential = Identity.getSignInClient(requireContext())
                        .getSignInCredentialFromIntent(intent)
                    val idToken = credential.googleIdToken
                    if (idToken != null) {
                        firebaseAuthWithGoogle(idToken)
                    } else {
                        Toast.makeText(requireContext(), "Sign-in failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Sign-in canceled", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        firebaseAuth = FirebaseAuth.getInstance()
        signInClient = Identity.getSignInClient(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val loadingScreen = LoadingScreen(requireContext())

        binding.Loginwithgmail.setOnClickListener {
            if (NetworkUtils.isNetworkAvailable(requireContext())) {
                loadingScreen.show()
                initiateGoogleSignIn { isSuccess ->
                    loadingScreen.dismiss()
                    if (!isSuccess) {
                        Toast.makeText(requireContext(), "Login failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "No Internet Connection", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initiateGoogleSignIn(onComplete: (Boolean) -> Unit) {
        val signInRequest = com.google.android.gms.auth.api.identity.BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                com.google.android.gms.auth.api.identity.BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .build()

        signInClient.beginSignIn(signInRequest)
            .addOnSuccessListener { result ->
                try {
                    signInLauncher.launch(
                        IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                    )
                    onComplete(true)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    onComplete(false)
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to initiate sign-in", Toast.LENGTH_SHORT).show()
                onComplete(false)
            }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val loadingScreen = LoadingScreen(requireContext())
        loadingScreen.show()

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                loadingScreen.dismiss()
                if (task.isSuccessful) {
                    firebaseAuth.currentUser?.getIdToken(true)
                        ?.addOnSuccessListener { result ->
                            val firebaseIdToken = result.token
                            if (firebaseIdToken != null) {
                                sendTokenToApi(firebaseIdToken)
                            }
                        }
                        ?.addOnFailureListener {
                            Toast.makeText(requireContext(), "Failed to retrieve token", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(requireContext(), "Authentication Failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun sendTokenToApi(token: String) {
        val client = ApiConfig.getApiService(token)
        client.getLoginMessage().enqueue(object : Callback<LoginResponse> {
            override fun onResponse(
                call: Call<LoginResponse>,
                response: Response<LoginResponse>
            ) {
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    Toast.makeText(
                        requireContext(),
                        loginResponse?.message ?: "Welcome to ReNote!",
                        Toast.LENGTH_LONG
                    ).show()
                    saveTokenLocally(token)
                    navigateToMainActivity()
                } else {
                    Toast.makeText(requireContext(), "Failed to fetch message", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveTokenLocally(token: String) {
        val sharedPreferences =
            requireContext().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("userToken", token)
            apply()
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(requireContext(), MainActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

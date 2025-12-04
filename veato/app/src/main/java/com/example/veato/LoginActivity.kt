package com.example.veato.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.example.veato.databinding.ActivityLoginBinding
import com.example.veato.MyTeamsActivity
import com.example.veato.OnboardingActivity
import com.example.veato.data.remote.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.example.veato.R

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { Firebase.firestore }
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener { login() }
        binding.tvGotoSignup.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        binding.tvForgot.setOnClickListener { sendReset() }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)


        binding.btnGoogle.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun signInWithGoogle() {
        googleSignInClient.signOut() // forces chooser next time
            .addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_SIGN_IN)
            }
    }


    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                // Error code 12501 = SIGN_IN_CANCELLED (user pressed back or dismissed the sheet)
                // Don't show toast for user-initiated cancellation
                if (e.statusCode != 12501) {
                    Toast.makeText(this, "Google sign-in failed: ${e.statusCode}", Toast.LENGTH_SHORT).show()
                }
                // Silently return to login screen when user cancels
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                    lifecycleScope.launch {
                        navigateBasedOnOnboardingStatus(userId)
                    }
                } else {
                    Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onStart() {
        super.onStart()

        val currentUser = auth.currentUser

        if (currentUser == null) {
            Log.d("AuthDebug", "No logged-in user → stay on LoginActivity")
            return
        }

        Log.d("AuthDebug", "Logged-in user detected: ${currentUser.uid}")

        lifecycleScope.launch {
            navigateBasedOnOnboardingStatus(currentUser.uid)
        }
    }


    private suspend fun navigateBasedOnOnboardingStatus(userId: String) {
        try {
            Log.d("AuthDebug", "Fetching user document for userId: $userId")
            val userDoc = db.collection("users").document(userId).get().await()
            val hasCompletedOnboarding = userDoc.getBoolean("onboardingCompleted") ?: false
            Log.d("AuthDebug", "Onboarding completed: $hasCompletedOnboarding")

            if (hasCompletedOnboarding) {
                Log.d("AuthDebug", "Navigating to MyTeamsActivity")
                // Go to main app if onboarding already completed
                startActivity(Intent(this, MyTeamsActivity::class.java))
            } else {
                Log.d("AuthDebug", "Navigating to OnboardingActivity")
                // Go to onboarding if not completed
                startActivity(Intent(this, OnboardingActivity::class.java))
            }
            finish()
        } catch (e: Exception) {
            Log.e("AuthDebug", "Error checking onboarding status: ${e.message}", e)
            // If error checking status, default to onboarding
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
        }
    }

    private fun login() {
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()

        if (email.isEmpty() || password.isEmpty()) {
            toast("Email and password are required")
            return
        }

        binding.progress.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false

        Log.d("AuthDebug", "Attempting sign in with email: $email")

        // Add a timeout check to see if request is hanging
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            Log.e("AuthDebug", "⏰ 10 second timeout reached - Firebase Auth callback never fired!")
        }, 10000)

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                Log.d("AuthDebug", "Sign in successful! AuthResult: $authResult")
                binding.progress.visibility = View.GONE
                binding.btnLogin.isEnabled = true

                val userId = auth.currentUser?.uid
                Log.d("AuthDebug", "User ID: $userId")
                if (userId != null) {
                    lifecycleScope.launch {
                        Log.d("AuthDebug", "Navigating based on onboarding status")
                        navigateBasedOnOnboardingStatus(userId)
                    }
                } else {
                    Log.e("AuthDebug", "User ID is null after successful login!")
                    toast("Login error: User ID is null")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("AuthDebug", "Sign in failed: ${exception.localizedMessage}", exception)
                binding.progress.visibility = View.GONE
                binding.btnLogin.isEnabled = true
                toast(exception.localizedMessage ?: "Login failed")
            }
            .addOnCompleteListener { task ->
                Log.d("AuthDebug", "Sign in completed. Success: ${task.isSuccessful}, Exception: ${task.exception}")
            }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun sendReset() {
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        if (email.isEmpty() || !email.contains("@")) {
            toast("Enter a valid email")
            return
        }

        binding.progress.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.checkEmailApiService
                    .checkEmail(CheckEmailRequest(email))

                if (response.exists == true) {
                    // Email exists -> send reset
                    FirebaseAuth.getInstance()
                        .sendPasswordResetEmail(email)
                        .addOnCompleteListener {
                            toast("Reset password link has been sent to the registered email")
                        }
                } else {
                    // Email not found
                    toast("This email is not registered")
                }

            } catch (e: Exception) {
                toast("Server error: ${e.message}")
            }

            binding.progress.visibility = View.GONE
            binding.btnLogin.isEnabled = true
        }
    }
}

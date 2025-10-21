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
import com.example.veato.ui.main.MainActivity
import com.example.veato.OnboardingActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.util.Log

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { Firebase.firestore }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener { login() }
        binding.tvGotoSignup.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        binding.tvForgot.setOnClickListener { sendReset() }
    }

//    override fun onStart() {
//        super.onStart()
//        // If already logged in, check if onboarding is complete
//        auth.currentUser?.let { user ->
//            lifecycleScope.launch {
//                navigateBasedOnOnboardingStatus(user.uid)
//            }
//        }
//    }

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
            val userDoc = db.collection("users").document(userId).get().await()
            val hasCompletedOnboarding = userDoc.getBoolean("onboardingCompleted") ?: false

            if (hasCompletedOnboarding) {
                // Go to main app if onboarding already completed
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                // Go to onboarding if not completed
                startActivity(Intent(this, OnboardingActivity::class.java))
            }
            finish()
        } catch (e: Exception) {
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

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                binding.progress.visibility = View.GONE
                binding.btnLogin.isEnabled = true
                if (task.isSuccessful) {
                    // Check onboarding status after successful login
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        lifecycleScope.launch {
                            navigateBasedOnOnboardingStatus(userId)
                        }
                    }
                } else {
                    toast(task.exception?.localizedMessage ?: "Login failed")
                }
            }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun sendReset() {
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        if (email.isEmpty() || !email.contains("@")) {
            toast("Enter a valid email to receive a reset link")
            return
        }
        binding.progress.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false

        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
            .addOnCompleteListener {
                binding.progress.visibility = View.GONE
                binding.btnLogin.isEnabled = true
                // Always show a generic message (prevents user enumeration)
                toast("Reset link has been sent if registered")
            }
    }
}

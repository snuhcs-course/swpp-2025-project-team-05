package com.example.veato.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.example.veato.databinding.ActivityRegisterBinding
import com.example.veato.model.User
import com.example.veato.ui.main.MainActivity
import com.example.veato.OnboardingActivity
import com.google.firebase.firestore.FieldValue

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { Firebase.firestore }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCreate.setOnClickListener { register() }
    }

    private fun register() {
        val fullName = binding.etFullName.text?.toString()?.trim().orEmpty()
        val usernameRaw = binding.etUsername.text?.toString()?.trim().orEmpty()
        val username = usernameRaw.removePrefix("@").lowercase()
        val email = binding.etRegEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etRegPassword.text?.toString().orEmpty()
        val confirm = binding.etRegConfirm.text?.toString().orEmpty()

        Log.d("AuthDebug", "Register button clicked - validating input")
        Log.d("AuthDebug", "Full name: $fullName, Username: $username, Email: $email")

        // Basic validation
        val usernameOk = username.matches(Regex("^[a-z0-9_]{3,20}$"))
        when {
            fullName.isEmpty() -> {
                Log.d("AuthDebug", "Validation failed: Full name is empty")
                err("Full name required")
            }
            !usernameOk -> {
                Log.d("AuthDebug", "Validation failed: Invalid username format")
                err("Invalid username (use 3–20 letters/numbers/_)")
            }
            email.isEmpty() -> {
                Log.d("AuthDebug", "Validation failed: Email is empty")
                err("Email required")
            }
            !email.contains("@") -> {
                Log.d("AuthDebug", "Validation failed: Email doesn't contain @")
                err("Email must be a valid email address (e.g., user@example.com)")
            }
            password.length < 6 -> {
                Log.d("AuthDebug", "Validation failed: Password too short")
                err("Password must be at least 6 chars")
            }
            password != confirm -> {
                Log.d("AuthDebug", "Validation failed: Passwords don't match")
                err("Passwords do not match")
            }
            else -> {
                Log.d("AuthDebug", "Validation passed - proceeding to create account")
                createAccount(fullName, username, email, password)
            }
        }
    }

    private fun createAccount(fullName: String, username: String, email: String, password: String) {
        busy(true)
        Log.d("AuthDebug", "Attempting to create account with email: $email, username: $username")

        // Add a timeout check to see if request is hanging
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            Log.e("AuthDebug", "⏰ 10 second timeout reached - Firebase Auth callback never fired!")
        }, 10000)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                Log.d("AuthDebug", "Account creation successful! AuthResult: $authResult")
                val uid = authResult.user?.uid
                Log.d("AuthDebug", "User UID: $uid")
                if (uid == null) {
                    Log.e("AuthDebug", "UID is null after successful account creation")
                    return@addOnSuccessListener err("No UID")
                }
                // Transaction: ensure username is unique and create both docs atomically
                val usernamesRef = db.collection("usernames").document(username)
                val usersRef = db.collection("users").document(uid)
                Log.d("AuthDebug", "Starting Firestore transaction for username: $username")

                db.runTransaction { tr ->
                    // ensure username is still free
                    if (tr.get(usernamesRef).exists()) {
                        throw IllegalStateException("Username already taken")
                    }

                    // reserve the username (uid must match auth.uid per rules)
                    tr.set(usernamesRef, mapOf("uid" to uid))

                    // create the user profile with SERVER TIMESTAMP
                    val profile = mapOf(
                        "uid" to uid,
                        "fullName" to fullName,
                        "username" to username,
                        "email" to email,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    tr.set(usersRef, profile)

                    null
                }.addOnSuccessListener {
                    Log.d("AuthDebug", "Firestore transaction successful - navigating to Onboarding")
                    startActivity(Intent(this, OnboardingActivity::class.java))
                    finish()
                }.addOnFailureListener { e ->
                    Log.e("AuthDebug", "Firestore transaction failed: ${e.localizedMessage}", e)
                    // Rollback auth account if username collision happens
                    auth.currentUser?.delete()
                    err(e.localizedMessage ?: "Sign up failed")
                }.addOnCompleteListener { task ->
                    Log.d("AuthDebug", "Transaction completed. Success: ${task.isSuccessful}, Exception: ${task.exception}")
                    busy(false)
                }

            }
            .addOnFailureListener { e ->
                Log.e("AuthDebug", "Account creation failed: ${e.localizedMessage}", e)
                busy(false)
                err(e.localizedMessage ?: "Sign up failed")
            }
            .addOnCompleteListener { task ->
                Log.d("AuthDebug", "createUserWithEmailAndPassword completed. Success: ${task.isSuccessful}, Exception: ${task.exception}")
            }
    }

    private fun busy(isBusy: Boolean) {
        binding.regProgress.visibility = if (isBusy) View.VISIBLE else View.GONE
        binding.btnCreate.isEnabled = !isBusy
    }

    private fun err(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}

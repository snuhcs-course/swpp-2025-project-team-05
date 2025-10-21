package com.example.veato.ui.auth

import android.content.Intent
import android.os.Bundle
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

        // Basic validation
        val usernameOk = username.matches(Regex("^[a-z0-9_]{3,20}$"))
        when {
            fullName.isEmpty() -> err("Full name required")
            !usernameOk -> err("Invalid username (use 3–20 letters/numbers/_)")
            email.isEmpty() -> err("Email required")
            password.length < 6 -> err("Password must be at least 6 chars")
            password != confirm -> err("Passwords do not match")
            else -> createAccount(fullName, username, email, password)
        }
    }

    private fun createAccount(fullName: String, username: String, email: String, password: String) {
        busy(true)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid ?: return@addOnSuccessListener err("No UID")
                // Transaction: ensure username is unique and create both docs atomically
                val usernamesRef = db.collection("usernames").document(username)
                val usersRef = db.collection("users").document(uid)

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
                    startActivity(Intent(this, OnboardingActivity::class.java))
                    finish()
                }.addOnFailureListener { e ->
                    // Rollback auth account if username collision happens
                    auth.currentUser?.delete()
                    err(e.localizedMessage ?: "Sign up failed")
                }.addOnCompleteListener { busy(false) }

            }
            .addOnFailureListener { e ->
                busy(false)
                err(e.localizedMessage ?: "Sign up failed")
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

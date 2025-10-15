package com.example.veato.ui.auth
import com.example.veato.R

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.veato.databinding.ActivityLoginBinding
import com.example.veato.ui.main.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

// Google Sign in
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_GOOGLE_SIGN_IN = 1001

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
            .requestIdToken(getString(R.string.default_web_client_id)) // from google-services.json
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.btnGoogle.setOnClickListener { signInWithGoogle() }
    }

    override fun onStart() {
        super.onStart()
        // If already logged in, go straight to main
        auth.currentUser?.let {
            startActivity(Intent(this, MainActivity::class.java))
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
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    toast(task.exception?.localizedMessage ?: "Login failed")
                }
            }
    }

    private fun sendReset() {
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            toast("Enter a valid email to receive a reset link")
            return
        }

        binding.progress.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false

        // Change this to backend URL (Render)
        val url = "https://veato-1.onrender.com/check-email"
        val json = JSONObject().apply { put("email", email) }

        val request = JsonObjectRequest(
            Request.Method.POST, url, json,
            { response ->
                val exists = response.optBoolean("exists", false)
                binding.progress.visibility = View.GONE
                binding.btnLogin.isEnabled = true

                if (!exists) {
                    toast("Email not registered")
                } else {
                    FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                toast("Reset link sent to your email")
                            } else {
                                toast("Failed: ${task.exception?.localizedMessage}")
                            }
                    }
                }
            },
            { error ->
                binding.progress.visibility = View.GONE
                binding.btnLogin.isEnabled = true
                toast("Server error: ${error.localizedMessage}")
            }
        )
        Volley.newRequestQueue(this).add(request)
    }

    private fun signInWithGoogle() {
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN)
        }
    }


    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_GOOGLE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                toast("Google sign-in failed: ${e.message}")
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        binding.progress.visibility = View.VISIBLE

        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                binding.progress.visibility = View.GONE
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    // Save to Firestore if first login
                    checkAndSaveUser(user)
                } else {
                    toast("Authentication Failed: ${task.exception?.message}")
                }
            }
    }

    private fun checkAndSaveUser(user: FirebaseUser?) {
        if (user == null) return

        val db = Firebase.firestore
        val usersRef = db.collection("users").document(user.uid)

        usersRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                val profile = mapOf(
                    "uid" to user.uid,
                    "fullName" to (user.displayName ?: ""),
                    "username" to (user.email?.substringBefore("@") ?: ""),
                    "email" to (user.email ?: ""),
                    "createdAt" to FieldValue.serverTimestamp()
                )
                usersRef.set(profile)
            }
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

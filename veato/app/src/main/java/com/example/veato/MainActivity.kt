package com.example.veato.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.veato.ProfileActivity
import com.google.firebase.auth.FirebaseAuth
import com.example.veato.databinding.ActivityMainBinding
import com.example.veato.ui.auth.LoginActivity

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            val i = Intent(this, LoginActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(i)
            finish()
        }

        binding.btnProfile.setOnClickListener {
            val i = Intent(this, ProfileActivity::class.java)
            startActivity(i)


        }
    }
}

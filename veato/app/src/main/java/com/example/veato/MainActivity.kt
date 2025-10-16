package com.example.veato.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.example.veato.databinding.ActivityMainBinding
import com.example.veato.ui.auth.LoginActivity
import com.example.veato.MyTeamsActivity
import com.example.veato.R

class MainActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val btnMyTeams = findViewById<Button>(R.id.btnMyTeams)

        val user = auth.currentUser
        tvWelcome.text = "Welcome to Veato, ${user?.email ?: "User"}"

        btnLogout.setOnClickListener {
            auth.signOut()
            finish()
        }

        btnMyTeams.setOnClickListener {
            val intent = Intent(this, MyTeamsActivity::class.java)
            startActivity(intent)
        }
    }
}


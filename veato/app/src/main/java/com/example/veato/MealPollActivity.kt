package com.example.veato

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MealPollActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meal_poll)

        val teamId = intent.getStringExtra("teamId")
        val title = findViewById<TextView>(R.id.tvPollTitle)
        val btnBack = findViewById<Button>(R.id.btnBack)

        title.text = "Meal Poll for Team: $teamId"

        btnBack.setOnClickListener {
            finish() // go back to MyTeamsActivity
        }
    }
}

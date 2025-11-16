package com.example.veato.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.veato.MyPreferencesActivity
import com.example.veato.MyTeamsActivity
import com.example.veato.ProfileActivity
import com.example.veato.ui.auth.LoginActivity
import com.example.veato.ui.theme.VeatoTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VeatoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainScreen() {
        val context = LocalContext.current
        val user = auth.currentUser

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Home",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        Button(
                            onClick = {
                                auth.signOut()
                                val intent = Intent(context, LoginActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                context.startActivity(intent)
                                finish()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Log out")
                        }
                    }
                )
            },
            bottomBar = {
                BottomNavigationBar(currentScreen = "Home")
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = "Welcome to Veato,\n${user?.email ?: "User"}",
                    fontSize = 18.sp,
                    color = Color.Black
                )
            }
        }
    }

    @Composable
    private fun BottomNavigationBar(currentScreen: String) {
        val context = LocalContext.current

        // Exact match to ProfileActivity navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(Color(0xFFE8F5E9))
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // My Preferences
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .clickable {
                        val intent = Intent(context, MyPreferencesActivity::class.java)
                        context.startActivity(intent)
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "My Preferences",
                    fontSize = 14.sp,
                    fontWeight = if (currentScreen == "Preferences") FontWeight.Bold else FontWeight.Normal,
                    color = Color.Black
                )
            }

            // My Teams
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .clickable {
                        val intent = Intent(context, MyTeamsActivity::class.java)
                        context.startActivity(intent)
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "My Teams",
                    fontSize = 14.sp,
                    fontWeight = if (currentScreen == "MyTeams") FontWeight.Bold else FontWeight.Normal,
                    color = Color.Black
                )
            }

            // My Profile
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .clickable {
                        val intent = Intent(context, ProfileActivity::class.java)
                        context.startActivity(intent)
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "My Profile",
                    fontSize = 14.sp,
                    fontWeight = if (currentScreen == "Profile") FontWeight.Bold else FontWeight.Normal,
                    color = Color.Black
                )
            }
        }
    }
}

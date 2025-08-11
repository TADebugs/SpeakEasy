package com.example.coursework.speakeasy.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if user is already logged in
        val sharedPrefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userName = sharedPrefs.getString("user_name", null)
        val isGuest = sharedPrefs.getBoolean("is_guest", false)

        if (userName != null && !isGuest) {
            // User is logged in, redirect to DashboardActivity
            startActivity(Intent(this, DashboardActivity::class.java))
        } else {
            // User is not logged in, redirect to CreateProfileActivity
            startActivity(Intent(this, CreateProfileActivity::class.java))
        }

        // Finish MainActivity to prevent returning to it
        finish()
    }
}
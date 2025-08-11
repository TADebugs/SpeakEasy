package com.example.coursework.speakeasy.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.coursework.speakeasy.R
import com.example.coursework.speakeasy.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {
    private val userDao by lazy { AppDatabase.getDatabase(this).userDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val sharedPrefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userName = sharedPrefs.getString("user_name", "User")
        val isGuest = sharedPrefs.getBoolean("is_guest", false)

        // Update greeting text
        val greetingText = if (isGuest) {
            "Welcome, Guest!"
        } else {
            "Hi, $userName!"
        }

        findViewById<TextView>(R.id.tvGreeting).text = greetingText

        // Profile button click
        findViewById<ImageButton>(R.id.btnProfile).setOnClickListener {
            showProfileDialog()
        }

        val btnChangePassword = findViewById<Button>(R.id.btnChangePassword)
        btnChangePassword.setOnClickListener {
            val etNewPassword = findViewById<EditText>(R.id.etNewPassword)
            val newPassword = etNewPassword.text.toString().trim()

            if (newPassword.isNotEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    val sharedPrefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    val username = sharedPrefs.getString("user_name", null)

                    if (username != null) {
                        val user = userDao.getUserByUsername(username)
                        if (user != null) {
                            userDao.updateUser(user.copy(password = newPassword))
                            runOnUiThread {
                                Toast.makeText(this@HomeActivity, "Password updated successfully!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Please enter a new password.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showProfileDialog() {
        val sharedPrefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val currentName = sharedPrefs.getString("user_name", "User")
        val isGuest = sharedPrefs.getBoolean("is_guest", false)

        val options = if (isGuest) {
            arrayOf("Set Name", "Logout")
        } else {
            arrayOf("Change Name", "Logout")
        }

        AlertDialog.Builder(this)
            .setTitle("Profile Settings")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showChangeNameDialog(currentName, isGuest)
                    1 -> logout()
                }
            }
            .show()
    }

    private fun showChangeNameDialog(currentName: String?, isGuest: Boolean) {
        val input = android.widget.EditText(this)
        if (!isGuest) {
            input.setText(currentName)
        }

        val dialogTitle = if (isGuest) "Set Your Name" else "Change Name"

        AlertDialog.Builder(this)
            .setTitle(dialogTitle)
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    // Save new name
                    getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .putString("user_name", newName)
                        .putBoolean("is_guest", false)
                        .apply()
                    
                    // Update greeting
                    findViewById<TextView>(R.id.tvGreeting).text = "Hi, $newName!"
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun logout() {
        // Clear saved data
        getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
        
        // Go back to login
        startActivity(Intent(this, CreateProfileActivity::class.java))
        finish()
    }
}

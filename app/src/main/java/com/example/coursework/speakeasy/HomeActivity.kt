package com.example.coursework.speakeasy

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.coursework.speakeasy.R
import com.example.coursework.speakeasy.database.AppDatabase
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {
    
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        
        // Initialize database
        database = AppDatabase.getDatabase(this)

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
    }

    private fun showProfileDialog() {
        val sharedPrefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val currentName = sharedPrefs.getString("user_name", "User")
        val isGuest = sharedPrefs.getBoolean("is_guest", false)

        val options = if (isGuest) {
            arrayOf("Set Name", "Logout")
        } else {
            arrayOf("Change Name", "Change Password", "Logout")
        }

        AlertDialog.Builder(this)
            .setTitle("Profile Settings")
            .setItems(options) { _, which ->
                if (isGuest) {
                    when (which) {
                        0 -> showChangeNameDialog(currentName, isGuest)
                        1 -> logout()
                    }
                } else {
                    when (which) {
                        0 -> showChangeNameDialog(currentName, isGuest)
                        1 -> showChangePasswordDialog()
                        2 -> logout()
                    }
                }
            }
            .show()
    }

    private fun showChangeNameDialog(currentName: String?, isGuest: Boolean) {
        val input = EditText(this)
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
    
    private fun showChangePasswordDialog() {
        val sharedPrefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val currentUsername = sharedPrefs.getString("username", "")
        
        if (currentUsername.isNullOrEmpty()) {
            Toast.makeText(this, "Error: User information not found", Toast.LENGTH_SHORT).show()
            return
        }

        // Create a custom layout for current and new password
        val dialogView = layoutInflater.inflate(android.R.layout.simple_form_dialog, null)
        val currentPasswordInput = EditText(this).apply {
            hint = "Current Password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val newPasswordInput = EditText(this).apply {
            hint = "New Password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val confirmPasswordInput = EditText(this).apply {
            hint = "Confirm New Password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            addView(currentPasswordInput)
            addView(newPasswordInput)
            addView(confirmPasswordInput)
        }

        AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setView(container)
            .setPositiveButton("Change") { _, _ ->
                val currentPassword = currentPasswordInput.text.toString().trim()
                val newPassword = newPasswordInput.text.toString().trim()
                val confirmPassword = confirmPasswordInput.text.toString().trim()
                
                if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                if (newPassword != confirmPassword) {
                    Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                if (newPassword.length < 4) {
                    Toast.makeText(this, "Password must be at least 4 characters", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                performPasswordChange(currentUsername, currentPassword, newPassword)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun performPasswordChange(username: String, currentPassword: String, newPassword: String) {
        lifecycleScope.launch {
            try {
                // First verify current password
                val user = database.userDao().loginUser(username, currentPassword)
                if (user == null) {
                    Toast.makeText(this@HomeActivity, 
                        "Current password is incorrect", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                // Update password
                val rowsUpdated = database.userDao().updatePassword(username, newPassword)
                if (rowsUpdated > 0) {
                    Toast.makeText(this@HomeActivity, 
                        "Password changed successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@HomeActivity, 
                        "Failed to change password. Please try again.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@HomeActivity, 
                    "Error changing password: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
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

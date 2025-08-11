package com.example.coursework.speakeasy.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.coursework.speakeasy.R
import com.example.coursework.speakeasy.data.AppDatabase
import com.example.coursework.speakeasy.data.User
import com.google.android.material.chip.Chip
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {
    private val userDao by lazy { AppDatabase.getDatabase(this).userDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val sharedPrefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userName = sharedPrefs.getString("user_name", "User")
        val isGuest = sharedPrefs.getBoolean("is_guest", false)

        // Update user name display
        val greetingText = if (isGuest) {
            "Hi Guest!"
        } else {
            "Hi $userName!"
        }
        findViewById<TextView>(R.id.tvUserName).text = greetingText

        // Set up button click listeners
        setupButtonListeners()
    }

    private fun setupButtonListeners() {
        // Profile button (now includes switch user functionality)
        findViewById<ImageButton>(R.id.btnProfile).setOnClickListener {
            showProfileDialog()
        }

        // Main action buttons
        findViewById<Button>(R.id.btnNewRecording).setOnClickListener {
            val intent = Intent(this, RecordingActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnNewPrompt).setOnClickListener {
            val intent = Intent(this, PromptActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnPriorRecordings).setOnClickListener {
            val intent = Intent(this, RecordingsListActivity::class.java)
            startActivity(intent)
        }

        // Chip buttons
        findViewById<Chip>(R.id.chipPastScores).setOnClickListener {
            // TODO: Implement past scores view
            Toast.makeText(this, "Past Scores feature coming soon!", Toast.LENGTH_SHORT).show()
        }

        findViewById<Chip>(R.id.chipHistory).setOnClickListener {
            // TODO: Implement history view
            Toast.makeText(this, "History feature coming soon!", Toast.LENGTH_SHORT).show()
        }

        findViewById<Chip>(R.id.chipPrompts).setOnClickListener {
            // TODO: Implement prompts management
            Toast.makeText(this, "Prompts Management feature coming soon!", Toast.LENGTH_SHORT).show()
        }

        findViewById<Chip>(R.id.chipProfile).setOnClickListener {
            showProfileDialog()
        }
    }

    private fun showProfileDialog() {
        val sharedPrefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val currentName = sharedPrefs.getString("user_name", "User")
        val isGuest = sharedPrefs.getBoolean("is_guest", false)

        val options = if (isGuest) {
            arrayOf("Set Name", "Change Password", "Switch User", "Logout")
        } else {
            arrayOf("Change Name", "Change Password", "Switch User", "Logout")
        }

        AlertDialog.Builder(this)
            .setTitle("Profile Settings")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showChangeNameDialog(currentName, isGuest)
                    1 -> showChangePasswordDialog()
                    2 -> switchUser()
                    3 -> logout()
                }
            }
            .show()
    }

    private fun showChangeNameDialog(currentName: String?, isGuest: Boolean) {
        // TODO: Implement name change dialog
        Toast.makeText(this, "Name change feature coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun showChangePasswordDialog() {
        val sharedPrefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userName = sharedPrefs.getString("user_name", "")
        val isGuest = sharedPrefs.getBoolean("is_guest", false)

        if (isGuest) {
            Toast.makeText(this, "Guest users cannot change password. Please login first.", Toast.LENGTH_LONG).show()
            return
        }

        // Create a dialog for password change
        val etCurrentPassword = android.widget.EditText(this).apply {
            hint = "Current Password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val etNewPassword = android.widget.EditText(this).apply {
            hint = "New Password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val etConfirmPassword = android.widget.EditText(this).apply {
            hint = "Confirm New Password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 20, 50, 20)
            addView(etCurrentPassword)
            addView(etNewPassword)
            addView(etConfirmPassword)
        }

        AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setView(layout)
            .setPositiveButton("Change") { _, _ ->
                val currentPassword = etCurrentPassword.text.toString()
                val newPassword = etNewPassword.text.toString()
                val confirmPassword = etConfirmPassword.text.toString()

                if (newPassword != confirmPassword) {
                    Toast.makeText(this, "New passwords do not match!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPassword.length < 6) {
                    Toast.makeText(this, "Password must be at least 6 characters!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Update password in database
                CoroutineScope(Dispatchers.IO).launch {
                    val user = userDao.getUserByUsername(userName ?: "")
                    if (user != null && user.password == currentPassword) {
                        val updatedUser = user.copy(password = newPassword)
                        userDao.updateUser(updatedUser)
                        runOnUiThread {
                            Toast.makeText(this@DashboardActivity, "Password changed successfully!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@DashboardActivity, "Current password is incorrect!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun switchUser() {
        // Clear shared preferences
        val sharedPrefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            clear()
            apply()
        }

        // Go back to login screen
        val intent = Intent(this, CreateProfileActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun logout() {
        // Clear shared preferences
        val sharedPrefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            clear()
            apply()
        }

        // Go back to login screen
        val intent = Intent(this, CreateProfileActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
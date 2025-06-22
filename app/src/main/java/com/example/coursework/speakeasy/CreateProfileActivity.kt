package com.example.coursework.speakeasy.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.coursework.speakeasy.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class CreateProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if user already has a REAL name (not guest)
        val sharedPrefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userName = sharedPrefs.getString("user_name", null)
        val isGuest = sharedPrefs.getBoolean("is_guest", false)
        
        // Only skip login if user has a real name (not guest)
        if (userName != null && !isGuest) {
            // User has a real name, go directly to HomeActivity
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            return
        }
        
        setContentView(R.layout.activity_create_profile)

        val etName = findViewById<TextInputEditText>(R.id.etName)
        val tilName = findViewById<TextInputLayout>(R.id.tilName)
        val btnContinue = findViewById<Button>(R.id.btnContinue)
        val btnGuest = findViewById<Button>(R.id.btnGuest)

        // Load animations
        val buttonPressAnimation = AnimationUtils.loadAnimation(this, R.anim.button_press)
        val inputFocusAnimation = AnimationUtils.loadAnimation(this, R.anim.input_focus)

        // Input field focus animation
        etName.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                tilName.startAnimation(inputFocusAnimation)
            }
        }

        // Continue button with animation
        btnContinue.setOnClickListener { view ->
            view.startAnimation(buttonPressAnimation)
            
            val name = etName.text.toString().trim()
            if (name.isNotEmpty()) {
                // Save to SharedPreferences
                getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString("user_name", name)
                    .putBoolean("is_guest", false)
                    .apply()
                // Go to HomeActivity
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
            }
        }

        // Guest button with animation
        btnGuest.setOnClickListener { view ->
            view.startAnimation(buttonPressAnimation)
            
            // Save guest status
            getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("user_name", "Guest")
                .putBoolean("is_guest", true)
                .apply()
            // Go to HomeActivity
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
    }
}

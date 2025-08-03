package com.example.coursework.speakeasy

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.coursework.speakeasy.R
import com.example.coursework.speakeasy.database.AppDatabase
import com.example.coursework.speakeasy.database.User
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class CreateProfileActivity : AppCompatActivity() {
    
    private lateinit var database: AppDatabase
    private var isLoginMode = true
    
    private lateinit var etName: TextInputEditText
    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var tilName: TextInputLayout
    private lateinit var tilUsername: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var btnContinue: Button
    private lateinit var btnToggleMode: Button
    private lateinit var btnGuest: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if user is already logged in
        val sharedPrefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val currentUserId = sharedPrefs.getInt("current_user_id", -1)
        val isGuest = sharedPrefs.getBoolean("is_guest", false)
        
        // Only skip login if user has a valid user ID (not guest)
        if (currentUserId != -1 && !isGuest) {
            // User is already logged in, go directly to HomeActivity
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            return
        }
        
        setContentView(R.layout.activity_create_profile)
        
        // Initialize database
        database = AppDatabase.getDatabase(this)
        
        // Initialize views
        initViews()
        
        // Set initial login mode
        updateUIForMode()
        
        // Set click listeners
        setClickListeners()
    }
    
    private fun initViews() {
        etName = findViewById(R.id.etName)
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        tilName = findViewById(R.id.tilName)
        tilUsername = findViewById(R.id.tilUsername)
        tilPassword = findViewById(R.id.tilPassword)
        btnContinue = findViewById(R.id.btnContinue)
        btnToggleMode = findViewById(R.id.btnToggleMode)
        btnGuest = findViewById(R.id.btnGuest)
    }
    
    private fun setClickListeners() {
        // Load animations
        val buttonPressAnimation = AnimationUtils.loadAnimation(this, R.anim.button_press)
        val inputFocusAnimation = AnimationUtils.loadAnimation(this, R.anim.input_focus)

        // Input field focus animations
        etName.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                tilName.startAnimation(inputFocusAnimation)
            }
        }
        
        etUsername.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                tilUsername.startAnimation(inputFocusAnimation)
            }
        }
        
        etPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                tilPassword.startAnimation(inputFocusAnimation)
            }
        }

        // Continue button (Login/Register)
        btnContinue.setOnClickListener { view ->
            view.startAnimation(buttonPressAnimation)
            
            if (isLoginMode) {
                performLogin()
            } else {
                performRegistration()
            }
        }
        
        // Toggle mode button
        btnToggleMode.setOnClickListener { view ->
            view.startAnimation(buttonPressAnimation)
            isLoginMode = !isLoginMode
            updateUIForMode()
        }

        // Guest button
        btnGuest.setOnClickListener { view ->
            view.startAnimation(buttonPressAnimation)
            
            // Save guest status
            getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("user_name", "Guest")
                .putBoolean("is_guest", true)
                .putInt("current_user_id", -1)
                .apply()
            // Go to HomeActivity
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
    }
    
    private fun updateUIForMode() {
        if (isLoginMode) {
            // Login mode
            tilName.visibility = View.GONE
            btnContinue.text = "Login"
            btnToggleMode.text = "Don't have an account? Register"
        } else {
            // Register mode
            tilName.visibility = View.VISIBLE
            btnContinue.text = "Register"
            btnToggleMode.text = "Already have an account? Login"
        }
    }
    
    private fun performLogin() {
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()
        
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                val user = database.userDao().loginUser(username, password)
                if (user != null) {
                    // Login successful
                    getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .putString("user_name", user.fullName)
                        .putString("username", user.username)
                        .putInt("current_user_id", user.id)
                        .putBoolean("is_guest", false)
                        .apply()
                    
                    startActivity(Intent(this@CreateProfileActivity, HomeActivity::class.java))
                    finish()
                } else {
                    // Login failed
                    Toast.makeText(this@CreateProfileActivity, 
                        "Invalid username or password. Please register if you don't have an account.", 
                        Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@CreateProfileActivity, 
                    "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun performRegistration() {
        val fullName = etName.text.toString().trim()
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()
        
        if (fullName.isEmpty() || username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                // Check if username already exists
                val existingUser = database.userDao().getUserByUsername(username)
                if (existingUser != null) {
                    Toast.makeText(this@CreateProfileActivity, 
                        "Username already exists. Please choose a different username.", 
                        Toast.LENGTH_LONG).show()
                    return@launch
                }
                
                // Register new user
                val newUser = User(username = username, password = password, fullName = fullName)
                val userId = database.userDao().registerUser(newUser)
                
                if (userId > 0) {
                    // Registration successful
                    getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .putString("user_name", fullName)
                        .putString("username", username)
                        .putInt("current_user_id", userId.toInt())
                        .putBoolean("is_guest", false)
                        .apply()
                    
                    Toast.makeText(this@CreateProfileActivity, 
                        "Registration successful! Welcome, $fullName!", Toast.LENGTH_SHORT).show()
                    
                    startActivity(Intent(this@CreateProfileActivity, HomeActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@CreateProfileActivity, 
                        "Registration failed. Please try again.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@CreateProfileActivity, 
                    "Registration failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

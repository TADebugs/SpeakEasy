package com.example.coursework.speakeasy.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.coursework.speakeasy.R
import com.example.coursework.speakeasy.data.AppDatabase
import com.example.coursework.speakeasy.data.User
import com.example.coursework.speakeasy.ui.DashboardActivity
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CreateProfileActivity : AppCompatActivity() {
    private val userDao by lazy { AppDatabase.getDatabase(this).userDao() }
    
    private lateinit var etFullName: TextInputEditText
    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnAction: Button
    private lateinit var btnToggleMode: TextView
    private lateinit var btnGuest: Button
    
    private var isLoginMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if user already has a REAL name (not guest)
        val sharedPrefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userName = sharedPrefs.getString("user_name", null)
        val isGuest = sharedPrefs.getBoolean("is_guest", false)

        // Only skip login if user has a real name (not guest)
        if (userName != null && !isGuest) {
            // User has a real name, go directly to DashboardActivity
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_create_profile)
        
        initializeViews()
        setupListeners()
        updateUI()
    }

    private fun initializeViews() {
        etFullName = findViewById(R.id.etName)
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnAction = findViewById(R.id.btnLogin)
        btnToggleMode = findViewById(R.id.btnToggleMode)
        btnGuest = findViewById(R.id.btnGuest)
    }
    
    private fun setupListeners() {
        // Load animations
        val buttonPressAnimation = AnimationUtils.loadAnimation(this, R.anim.button_press)

        btnAction.setOnClickListener { view ->
            view.startAnimation(buttonPressAnimation)
            
            if (isLoginMode) {
                handleLogin()
            } else {
                handleSignUp()
            }
        }

        btnToggleMode.setOnClickListener {
            isLoginMode = !isLoginMode
            updateUI()
        }

        btnGuest.setOnClickListener { view ->
            view.startAnimation(buttonPressAnimation)

            // Save guest info in shared preferences
            val sharedPrefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            with(sharedPrefs.edit()) {
                putString("user_name", "Guest")
                putBoolean("is_guest", true)
                apply()
            }

            Toast.makeText(this, "Continuing as Guest", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        }
    }
    
    private fun updateUI() {
        if (isLoginMode) {
            // Login mode
            etFullName.visibility = View.GONE
            btnAction.text = "Login"
            btnToggleMode.text = "Don't have an account? Sign Up"
        } else {
            // Sign Up mode
            etFullName.visibility = View.VISIBLE
            btnAction.text = "Sign Up"
            btnToggleMode.text = "Already have an account? Login"
        }
    }
    
    private fun handleLogin() {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if user exists and validate password
                CoroutineScope(Dispatchers.IO).launch {
                    val existingUser = userDao.getUserByUsername(username)
            
                    if (existingUser != null) {
                // User exists - check password
                        if (existingUser.password == password) {
                    // Login successful
                            val sharedPrefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                            with(sharedPrefs.edit()) {
                                putString("user_name", existingUser.username)
                        putString("full_name", existingUser.fullName)
                                putBoolean("is_guest", false)
                                apply()
                            }

                            runOnUiThread {
                                Toast.makeText(this@CreateProfileActivity, "Login successful!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this@CreateProfileActivity, DashboardActivity::class.java))
                                finish()
                            }
                        } else {
                            // Password is incorrect
                            runOnUiThread {
                                Toast.makeText(this@CreateProfileActivity, "Incorrect password. Please try again.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                // User doesn't exist
                        runOnUiThread {
                    Toast.makeText(this@CreateProfileActivity, "Username not found. Please sign up first.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun handleSignUp() {
        val fullName = etFullName.text.toString().trim()
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (fullName.isEmpty() || username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if user already exists
                CoroutineScope(Dispatchers.IO).launch {
            val existingUser = userDao.getUserByUsername(username)
            
            if (existingUser != null) {
                // User already exists
                            runOnUiThread {
                    Toast.makeText(this@CreateProfileActivity, "Username '$username' already signed up. Try logging in.", Toast.LENGTH_LONG).show()
                }
            } else {
                // Create new user
                val newUser = User(
                    fullName = fullName,
                    username = username,
                    password = password
                )
                
                try {
                    userDao.insertUser(newUser)
                    
                    // Save user info in shared preferences
                    val sharedPrefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    with(sharedPrefs.edit()) {
                        putString("user_name", username)
                        putString("full_name", fullName)
                        putBoolean("is_guest", false)
                        apply()
                    }

                            runOnUiThread {
                        Toast.makeText(this@CreateProfileActivity, "Registration successful! Welcome to SpeakEasy!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@CreateProfileActivity, DashboardActivity::class.java))
                        finish()
                            }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@CreateProfileActivity, "Registration failed. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
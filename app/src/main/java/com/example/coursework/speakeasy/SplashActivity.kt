package com.example.speakeasy.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.speakeasy.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        findViewById<FloatingActionButton>(R.id.btnStart).setOnClickListener {
            startActivity(Intent(this, CreateProfileActivity::class.java))
            finish()
        }
    }
}

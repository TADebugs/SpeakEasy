package com.example.coursework.speakeasy.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.coursework.speakeasy.R
import com.example.coursework.speakeasy.data.AppDatabase
import com.example.coursework.speakeasy.data.Prompt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PromptActivity : AppCompatActivity() {
    
    private lateinit var tvTitle: TextView
    private lateinit var btnRandomPrompt: Button
    private lateinit var btnNewTextScript: Button
    private lateinit var btnCreateCustom: Button
    private lateinit var btnCreateCustomScript: Button
    private lateinit var btnCancel: Button
    private lateinit var tvPromptTitle: TextView
    private lateinit var tvPromptContent: TextView
    private lateinit var tvScriptTitle: TextView
    private lateinit var tvScriptContent: TextView
    
    private val promptDao by lazy { AppDatabase.getDatabase(this).promptDao() }
    private val userDao by lazy { AppDatabase.getDatabase(this).userDao() }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prompt)
        
        initializeViews()
        setupListeners()
        clearAllContent()
        initializeDefaultPromptsAsync()
    }
    
    private fun initializeViews() {
        tvTitle = findViewById(R.id.tvTitle)
        btnRandomPrompt = findViewById(R.id.btnRandomPrompt)
        btnNewTextScript = findViewById(R.id.btnNewTextScript)
        btnCreateCustom = findViewById(R.id.btnCreateCustom)
        btnCreateCustomScript = findViewById(R.id.btnCreateCustomScript)
        btnCancel = findViewById(R.id.btnCancel)
        tvPromptTitle = findViewById(R.id.tvPromptTitle)
        tvPromptContent = findViewById(R.id.tvPromptContent)
        tvScriptTitle = findViewById(R.id.tvScriptTitle)
        tvScriptContent = findViewById(R.id.tvScriptContent)
    }
    
    private fun setupListeners() {
        btnRandomPrompt.setOnClickListener {
            getRandomPrompt()
        }
        
        btnNewTextScript.setOnClickListener {
            getRandomTextScript()
        }
        
        btnCreateCustom.setOnClickListener {
            showCreatePromptDialog()
        }
        
        btnCreateCustomScript.setOnClickListener {
            showCreateScriptDialog()
        }
        
        btnCancel.setOnClickListener {
            finish()
        }
    }
    

    
    private fun initializeDefaultPromptsAsync() {
        CoroutineScope(Dispatchers.IO).launch {
            val existingPrompts = promptDao.getAllSystemPrompts()
            
            // Only add default prompts if none exist
            if (existingPrompts.isEmpty()) {
                val defaultPrompts = listOf(
                    Prompt(
                        title = "My Greatest Achievement",
                        content = "Tell us about your greatest achievement and what you learned from it. Focus on the challenges you faced and how you overcame them.",
                        category = "Personal",
                        difficulty = "Easy",
                        estimatedDuration = 3
                    ),
                    Prompt(
                        title = "The Future of Technology",
                        content = "Discuss how technology will change our lives in the next 10 years. Consider both positive and negative impacts.",
                        category = "Technology",
                        difficulty = "Medium",
                        estimatedDuration = 4
                    ),
                    Prompt(
                        title = "Leadership in Crisis",
                        content = "Describe a time when you had to lead during a difficult situation. What strategies did you use and what was the outcome?",
                        category = "Business",
                        difficulty = "Hard",
                        estimatedDuration = 5
                    ),
                    Prompt(
                        title = "Cultural Diversity",
                        content = "Explain how cultural diversity benefits organizations and communities. Share specific examples.",
                        category = "Social",
                        difficulty = "Medium",
                        estimatedDuration = 4
                    ),
                    Prompt(
                        title = "Environmental Responsibility",
                        content = "What role should individuals and businesses play in protecting the environment? Discuss practical steps we can take.",
                        category = "Environmental",
                        difficulty = "Medium",
                        estimatedDuration = 4
                    )
                )
                
                defaultPrompts.forEach { prompt ->
                    promptDao.insertPrompt(prompt)
                }
            }
        }
    }
    
    private fun showCreatePromptDialog() {
        val sharedPrefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val username = sharedPrefs.getString("user_name", "")
        val isGuest = sharedPrefs.getBoolean("is_guest", false)
        
        if (isGuest) {
            Toast.makeText(this, "Please login to create custom prompts", Toast.LENGTH_SHORT).show()
            return
        }
        
        val etTitle = android.widget.EditText(this).apply {
            hint = "Prompt Title"
        }
        val etContent = android.widget.EditText(this).apply {
            hint = "Prompt Content"
            minLines = 3
        }
        val etCategory = android.widget.EditText(this).apply {
            hint = "Category (e.g., Personal, Business, Technology)"
        }
        
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 20, 50, 20)
            addView(etTitle)
            addView(etContent)
            addView(etCategory)
        }
        
        AlertDialog.Builder(this)
            .setTitle("Create New Prompt")
            .setView(layout)
            .setPositiveButton("Create") { _, _ ->
                val title = etTitle.text.toString().trim()
                val content = etContent.text.toString().trim()
                val category = etCategory.text.toString().trim()
                
                if (title.isNotEmpty() && content.isNotEmpty() && category.isNotEmpty()) {
                    createCustomPrompt(title, content, category, username ?: "")
                } else {
                    Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun createCustomPrompt(title: String, content: String, category: String, username: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val prompt = Prompt(
                title = title,
                content = content,
                category = category,
                isCustom = true,
                createdBy = username
            )
            
            val promptId = promptDao.insertPrompt(prompt)
            
            withContext(Dispatchers.Main) {
                Toast.makeText(this@PromptActivity, "Custom prompt created!", Toast.LENGTH_SHORT).show()
                displayCurrentPrompt(prompt)
            }
        }
    }
    
    private fun showCreateScriptDialog() {
        val sharedPrefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val username = sharedPrefs.getString("user_name", "")
        val isGuest = sharedPrefs.getBoolean("is_guest", false)
        
        if (isGuest) {
            Toast.makeText(this, "Please login to create custom scripts", Toast.LENGTH_SHORT).show()
            return
        }
        
        val etTitle = android.widget.EditText(this).apply {
            hint = "Script Title"
        }
        val etContent = android.widget.EditText(this).apply {
            hint = "Script Content"
            minLines = 5
        }
        
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 20, 50, 20)
            addView(etTitle)
            addView(etContent)
        }
        
        AlertDialog.Builder(this)
            .setTitle("Create New Script")
            .setView(layout)
            .setPositiveButton("Create") { _, _ ->
                val title = etTitle.text.toString().trim()
                val content = etContent.text.toString().trim()
                
                if (title.isNotEmpty() && content.isNotEmpty()) {
                    createCustomScript(title, content, username ?: "")
                } else {
                    Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun createCustomScript(title: String, content: String, username: String) {
        // Display the custom script and clear prompt content
        displayCurrentScript(content)
        Toast.makeText(this, "Custom script created!", Toast.LENGTH_SHORT).show()
    }
    
    private fun clearAllContent() {
        tvPromptTitle.text = "Prompt"
        tvPromptContent.text = "Select a prompt to see the content here"
        tvScriptTitle.text = "Text Script"
        tvScriptContent.text = "Select a text script to see the content here"
    }
    
    private fun clearPromptDisplay() {
        tvPromptTitle.text = "Prompt"
        tvPromptContent.text = "Select a prompt to see the content here"
    }
    
    private fun clearScriptDisplay() {
        tvScriptTitle.text = "Text Script"
        tvScriptContent.text = "Select a text script to see the content here"
    }
    
    private fun getRandomPrompt() {
        CoroutineScope(Dispatchers.IO).launch {
            val randomPrompt = promptDao.getRandomSystemPrompt()
            
            withContext(Dispatchers.Main) {
                if (randomPrompt != null) {
                    // Display the prompt in the UI
                    displayCurrentPrompt(randomPrompt)
                    showPromptDialog(randomPrompt)
                } else {
                    Toast.makeText(this@PromptActivity, "No prompts available", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun getRandomTextScript() {
        val sampleScripts = listOf(
            "Welcome to our annual company meeting. Today, we'll be discussing our achievements from the past year and our goals for the future. I'm excited to share with you the incredible progress we've made as a team.",
            "Good morning everyone. I'm here today to present our quarterly sales report. We've seen remarkable growth in our key markets, and I want to walk you through the numbers and what they mean for our business strategy.",
            "Thank you for joining us today for this important presentation. We're going to explore how technology is transforming our industry and what opportunities this creates for our organization. Let me start by sharing some insights from our recent research.",
            "Ladies and gentlemen, it's an honor to speak to you today about innovation in education. As we look toward the future, we must consider how we can better prepare our students for the challenges and opportunities that lie ahead.",
            "Hello everyone, and welcome to our product launch event. Today marks an exciting milestone for our company as we introduce a revolutionary new solution that we believe will transform how people work and collaborate."
        )
        
        val randomScript = sampleScripts.random()
        displayCurrentScript(randomScript)
        showScriptDialog(randomScript)
    }
    
    private fun displayCurrentPrompt(prompt: Prompt) {
        // Clear script content first
        tvScriptTitle.text = "Text Script"
        tvScriptContent.text = "Select a text script to see the content here"
        
        // Display prompt content
        tvPromptTitle.text = "Prompt: ${prompt.title}"
        tvPromptContent.text = prompt.content
    }
    
    private fun displayCurrentScript(script: String) {
        // Clear prompt content first
        tvPromptTitle.text = "Prompt"
        tvPromptContent.text = "Select a prompt to see the content here"
        
        // Display script content
        tvScriptTitle.text = "Text Script"
        tvScriptContent.text = script
    }
    
    private fun showPromptDialog(prompt: Prompt) {
        AlertDialog.Builder(this)
            .setTitle(prompt.title)
            .setMessage(prompt.content)
            .setPositiveButton("Use This Prompt") { _, _ ->
                // TODO: Start recording with this prompt
                Toast.makeText(this, "Starting recording with: ${prompt.title}", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, RecordingActivity::class.java)
                intent.putExtra("prompt_id", prompt.id)
                intent.putExtra("prompt_title", prompt.title)
                intent.putExtra("prompt_content", prompt.content)
                startActivity(intent)
            }
            .setNegativeButton("Get Another") { _, _ ->
                // Get another random prompt
                getRandomPrompt()
            }
            .setNeutralButton("Cancel") { _, _ ->
                // Clear the prompt display and return to default state
                clearPromptDisplay()
            }
            .show()
    }
    
    private fun showScriptDialog(script: String) {
        AlertDialog.Builder(this)
            .setTitle("Text Script")
            .setMessage(script)
            .setPositiveButton("Use This Script") { _, _ ->
                Toast.makeText(this, "Starting recording with this script", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, RecordingActivity::class.java)
                intent.putExtra("script_content", script)
                startActivity(intent)
            }
            .setNegativeButton("Get Another") { _, _ ->
                // Get another random script
                getRandomTextScript()
            }
            .setNeutralButton("Cancel") { _, _ ->
                // Clear the script display and return to default state
                clearScriptDisplay()
            }
            .show()
    }
}
package com.example.coursework.speakeasy.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "prompts")
data class Prompt(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val title: String, // Title of the prompt
    val content: String, // The actual prompt text
    val category: String, // Category (e.g., "Business", "Personal", "Academic")
    val difficulty: String = "Medium", // Easy, Medium, Hard
    val estimatedDuration: Int = 3, // Estimated speaking time in minutes
    val isCustom: Boolean = false, // Whether it's a user-created prompt
    val createdBy: String? = null, // Username who created it (null for system prompts)
    val createdAt: Date = Date(), // When the prompt was created
    val tags: String? = null // Optional comma-separated tags
) 
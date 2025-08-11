package com.example.coursework.speakeasy.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "recordings")
data class Recording(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val userId: String, // Username of the user who created the recording
    val fileName: String, // Name of the audio file
    val filePath: String, // Full path to the audio file
    val title: String, // User-defined title for the recording
    val duration: Long, // Duration in milliseconds
    val fileSize: Long, // File size in bytes
    val createdAt: Date = Date(), // When the recording was created
    val prompt: String? = null, // Optional prompt used for the recording
    val notes: String? = null, // Optional user notes
    val score: Float? = null, // Optional score/rating (0.0 to 10.0)
    val tags: String? = null // Optional comma-separated tags
) 
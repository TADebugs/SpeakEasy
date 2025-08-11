package com.example.coursework.speakeasy.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.coursework.speakeasy.R
import com.example.coursework.speakeasy.data.AppDatabase
import com.example.coursework.speakeasy.data.Recording
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class RecordingsListActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvNoRecordings: TextView
    private lateinit var btnBack: Button
    
    private var recordingsAdapter: RecordingsAdapter? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentlyPlayingPosition: Int = -1
    
    private val recordingDao by lazy { AppDatabase.getDatabase(this).recordingDao() }
    private val PERMISSION_REQUEST_CODE = 124
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recordings_list)
        
        initializeViews()
        setupListeners()
        checkPermissions()
        loadRecordings()
    }
    
    private fun initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewRecordings)
        tvNoRecordings = findViewById(R.id.tvNoRecordings)
        btnBack = findViewById(R.id.btnBack)
        
        recyclerView.layoutManager = LinearLayoutManager(this)
    }
    
    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }
        
        // Setup volume control
        // Removed volume control setup as users can use device volume controls
    }
    
    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
            != PackageManager.PERMISSION_GRANTED) {
            
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_CODE
            )
        } else {
            // Permission already granted, load recordings
            loadRecordings()
        }
    }
    
    private fun loadRecordings() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = getCurrentUserId()
                val recordings = recordingDao.getRecordingsByUser(userId)
                
                // Debug: Check if files exist
                recordings.forEach { recording ->
                    val file = File(recording.filePath)
                    if (!file.exists()) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@RecordingsListActivity, "Warning: File not found: ${recording.filePath}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                
                withContext(Dispatchers.Main) {
                    if (recordings.isEmpty()) {
                        tvNoRecordings.visibility = TextView.VISIBLE
                        recyclerView.visibility = RecyclerView.GONE
                    } else {
                        tvNoRecordings.visibility = TextView.GONE
                        recyclerView.visibility = RecyclerView.VISIBLE
                        
                        recordingsAdapter = RecordingsAdapter(recordings) { recording, position ->
                            onRecordingClicked(recording, position)
                        }
                        recyclerView.adapter = recordingsAdapter
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RecordingsListActivity, "Error loading recordings: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun onRecordingClicked(recording: Recording, position: Int) {
        val options = arrayOf("Play", "Delete", "View Details")
        
        AlertDialog.Builder(this)
            .setTitle(recording.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> playRecording(recording, position)
                    1 -> deleteRecording(recording)
                    2 -> showRecordingDetails(recording)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun playRecording(recording: Recording, position: Int) {
        val file = File(recording.filePath)
        if (!file.exists()) {
            Toast.makeText(this, "Recording file not found at: ${recording.filePath}", Toast.LENGTH_LONG).show()
            return
        }
        
        try {
            // Try to request audio focus, but don't fail if denied
            val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            var audioFocusGranted = false
            
            try {
                // Use simpler audio focus request to avoid attribution issues
                val result = audioManager.requestAudioFocus(
                    object : AudioManager.OnAudioFocusChangeListener {
                        override fun onAudioFocusChange(focusChange: Int) {
                            when (focusChange) {
                                AudioManager.AUDIOFOCUS_LOSS -> {
                                    stopCurrentPlayback()
                                }
                                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                                    mediaPlayer?.pause()
                                }
                                AudioManager.AUDIOFOCUS_GAIN -> {
                                    mediaPlayer?.start()
                                }
                            }
                        }
                    },
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
                
                audioFocusGranted = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            } catch (e: Exception) {
                // If audio focus request fails, continue anyway
                audioFocusGranted = false
                android.util.Log.w("RecordingsListActivity", "Audio focus request failed: ${e.message}")
            }
            
            if (!audioFocusGranted) {
                Toast.makeText(this, "Audio focus not granted, but attempting playback anyway", Toast.LENGTH_SHORT).show()
            }
            
            // Stop currently playing recording
            stopCurrentPlayback()
            
            mediaPlayer = MediaPlayer().apply {
                try {
                    // Use better audio attributes for voice playback
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        val audioAttributes = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                        setAudioAttributes(audioAttributes)
                    } else {
                        @Suppress("DEPRECATION")
                        setAudioStreamType(AudioManager.STREAM_VOICE_CALL)
                    }
                    
                    setDataSource(file.absolutePath)
                    prepare()
                    
                    // Removed volume setting as users can use device volume controls
                    
                    start()
                    
                    currentlyPlayingPosition = position
                    recordingsAdapter?.setPlayingPosition(position)
                    
                    Toast.makeText(this@RecordingsListActivity, "Playing: ${recording.title}", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    val errorMsg = "Error starting playback: ${e.message}"
                    Toast.makeText(this@RecordingsListActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    android.util.Log.e("RecordingsListActivity", errorMsg, e)
                    currentlyPlayingPosition = -1
                    recordingsAdapter?.setPlayingPosition(-1)
                    // Clean up on error
                    reset()
                    release()
                    mediaPlayer = null
                }
                
                setOnCompletionListener {
                    currentlyPlayingPosition = -1
                    recordingsAdapter?.setPlayingPosition(-1)
                    // Abandon audio focus
                    try {
                        audioManager.abandonAudioFocus(null)
                    } catch (e: Exception) {
                        android.util.Log.w("RecordingsListActivity", "Error abandoning audio focus: ${e.message}")
                    }
                }
                
                setOnErrorListener { _, what, extra ->
                    val errorMsg = "Playback error: $what"
                    Toast.makeText(this@RecordingsListActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    android.util.Log.e("RecordingsListActivity", errorMsg)
                    currentlyPlayingPosition = -1
                    recordingsAdapter?.setPlayingPosition(-1)
                    // Abandon audio focus
                    try {
                        audioManager.abandonAudioFocus(null)
                    } catch (e: Exception) {
                        android.util.Log.w("RecordingsListActivity", "Error abandoning audio focus: ${e.message}")
                    }
                    true
                }
            }
        } catch (e: Exception) {
            val errorMsg = "Error playing recording: ${e.message}"
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            android.util.Log.e("RecordingsListActivity", errorMsg, e)
        }
    }
    
    private fun stopCurrentPlayback() {
        mediaPlayer?.apply {
            try {
                if (isPlaying) {
                    stop()
                }
            } catch (e: Exception) {
                // Ignore if already stopped
            }
            reset()
            release()
        }
        mediaPlayer = null
        currentlyPlayingPosition = -1
        recordingsAdapter?.setPlayingPosition(-1)
        
        // Abandon audio focus with simpler approach
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        try {
            audioManager.abandonAudioFocus(null)
        } catch (e: Exception) {
            android.util.Log.w("RecordingsListActivity", "Error abandoning audio focus: ${e.message}")
        }
    }
    
    private fun deleteRecording(recording: Recording) {
        AlertDialog.Builder(this)
            .setTitle("Delete Recording")
            .setMessage("Are you sure you want to delete '${recording.title}'?")
            .setPositiveButton("Delete") { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // Delete file
                        val file = File(recording.filePath)
                        if (file.exists()) {
                            file.delete()
                        }
                        
                        // Delete from database
                        recordingDao.deleteRecording(recording)
                        
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@RecordingsListActivity, "Recording deleted", Toast.LENGTH_SHORT).show()
                            loadRecordings() // Reload the list
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@RecordingsListActivity, "Error deleting recording", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showRecordingDetails(recording: Recording) {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
        val duration = formatDuration(recording.duration)
        val fileSize = formatFileSize(recording.fileSize)
        
        val details = """
            Title: ${recording.title}
            Date: ${dateFormat.format(recording.createdAt)}
            Duration: $duration
            File Size: $fileSize
            ${if (recording.prompt != null) "Prompt: ${recording.prompt}" else ""}
        """.trimIndent()
        
        AlertDialog.Builder(this)
            .setTitle("Recording Details")
            .setMessage(details)
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun formatDuration(durationMs: Long): String {
        val seconds = (durationMs / 1000).toInt()
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }
    
    private fun formatFileSize(sizeBytes: Long): String {
        val kb = sizeBytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1 -> String.format("%.1f MB", mb)
            else -> String.format("%.0f KB", kb)
        }
    }
    
    private fun getCurrentUserId(): String {
        val sharedPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        return sharedPrefs.getString("user_name", "unknown") ?: "unknown"
    }
    
    private fun saveVolumePreference(volume: Float) {
        val sharedPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        sharedPrefs.edit().putFloat("playback_volume", volume).apply()
    }
    
    private fun loadVolumePreference(): Float {
        val sharedPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        return sharedPrefs.getFloat("playback_volume", 0.3f)
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadRecordings()
            } else {
                Toast.makeText(this, "Permission required to access recordings", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopCurrentPlayback()
        
        // Additional cleanup
        mediaPlayer?.apply {
            try {
                if (isPlaying) {
                    stop()
                }
            } catch (e: Exception) {
                // Ignore if already stopped
            }
            reset()
            release()
        }
        mediaPlayer = null
    }
    
    // RecyclerView Adapter
    inner class RecordingsAdapter(
        private val recordings: List<Recording>,
        private val onItemClick: (Recording, Int) -> Unit
    ) : RecyclerView.Adapter<RecordingsAdapter.ViewHolder>() {
        
        private var playingPosition: Int = -1
        
        fun setPlayingPosition(position: Int) {
            val oldPosition = playingPosition
            playingPosition = position
            if (oldPosition >= 0) notifyItemChanged(oldPosition)
            if (playingPosition >= 0) notifyItemChanged(playingPosition)
        }
        
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvTitle: TextView = itemView.findViewById(R.id.tvRecordingTitle)
            private val tvDate: TextView = itemView.findViewById(R.id.tvRecordingDate)
            private val tvDuration: TextView = itemView.findViewById(R.id.tvRecordingDuration)
            private val ivPlaying: ImageView = itemView.findViewById(R.id.ivPlaying)
            
            fun bind(recording: Recording, position: Int) {
                tvTitle.text = recording.title
                tvDate.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(recording.createdAt)
                tvDuration.text = formatDuration(recording.duration)
                
                ivPlaying.visibility = if (position == playingPosition) View.VISIBLE else View.GONE
                
                itemView.setOnClickListener {
                    onItemClick(recording, position)
                }
            }
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_recording, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(recordings[position], position)
        }
        
        override fun getItemCount(): Int = recordings.size
    }
} 
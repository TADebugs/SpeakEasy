package com.example.coursework.speakeasy.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.coursework.speakeasy.R
import com.example.coursework.speakeasy.data.AppDatabase
import com.example.coursework.speakeasy.data.Recording
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class RecordingActivity : AppCompatActivity() {
    
    private lateinit var btnRecord: Button
    private lateinit var btnPlay: Button
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var btnDebug: Button
    private lateinit var btnMonitor: Button
    private lateinit var btnTestSources: Button
    private lateinit var btnTestConfigs: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvPromptTitle: TextView
    private lateinit var tvPromptContent: TextView
    
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isRecording = false
    private var isCurrentlyPlaying = false
    private var recordingFile: File? = null
    private var outputFile: String? = null
    private var recordingDuration: Long = 0
    private var recordingStartTime: Long = 0
    
    private val recordingDao by lazy { AppDatabase.getDatabase(this).recordingDao() }
    private val PERMISSION_REQUEST_CODE = 123
    
    // Timer variables
    private var timerHandler: android.os.Handler? = null
    private var timerRunnable: Runnable? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recording)
        
        initializeViews()
        setupListeners()
        checkPermissions()
    }
    
    private fun loadPromptFromIntent() {
        val promptTitle = intent.getStringExtra("prompt_title")
        val promptContent = intent.getStringExtra("prompt_content")
        val scriptContent = intent.getStringExtra("script_content")
        
        if (promptTitle != null && promptContent != null) {
            tvPromptTitle.text = "Prompt: $promptTitle"
            tvPromptContent.text = promptContent
        } else if (scriptContent != null) {
            tvPromptTitle.text = "Text Script"
            tvPromptContent.text = scriptContent
        } else {
            tvPromptTitle.text = "Speaking Prompt"
            tvPromptContent.text = "No prompt selected"
        }
    }
    
    private fun initializeViews() {
        btnRecord = findViewById(R.id.btnRecord)
        btnPlay = findViewById(R.id.btnPlay)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)
        btnDebug = findViewById(R.id.btnDebug)
        btnMonitor = findViewById(R.id.btnMonitor)
        btnTestSources = findViewById(R.id.btnTestSources)
        btnTestConfigs = findViewById(R.id.btnTestConfigs)
        tvStatus = findViewById(R.id.tvStatus)
        tvTimer = findViewById(R.id.tvTimer)
        tvPromptTitle = findViewById(R.id.tvPromptTitle)
        tvPromptContent = findViewById(R.id.tvPromptContent)
        
        // Initially disable play and save buttons
        btnPlay.isEnabled = false
        btnSave.isEnabled = false
        
        // Load prompt from intent extras
        loadPromptFromIntent()
    }
    
    private fun setupListeners() {
        btnRecord.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
        }
        
        btnPlay.setOnClickListener {
            if (isCurrentlyPlaying) {
                stopPlaying()
            } else {
                startPlaying()
            }
        }
        
        btnSave.setOnClickListener {
            saveRecording()
        }
        
        btnCancel.setOnClickListener {
            cancelRecording()
        }

        btnDebug.setOnClickListener {
            // Consolidate all debug info into one comprehensive log
            val debugSummary = buildString {
                appendLine("=== SPEAKEASY DEBUG SUMMARY ===")
                appendLine("Microphone Status:")
                appendLine("- Record Permission: ${ContextCompat.checkSelfPermission(this@RecordingActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED}")
                appendLine("- Write Permission: ${ContextCompat.checkSelfPermission(this@RecordingActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED}")
                appendLine("- Read Permission: ${ContextCompat.checkSelfPermission(this@RecordingActivity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED}")
                appendLine("- Microphone Feature: ${packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_MICROPHONE)}")
                
                appendLine("\nAudio Levels:")
                val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
                appendLine("- Music Max Volume: ${audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)}")
                appendLine("- Music Current Volume: ${audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)}")
                appendLine("- Voice Call Max Volume: ${audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)}")
                appendLine("- Voice Call Current Volume: ${audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL)}")
                
                appendLine("\nRecording Status:")
                appendLine("- Is Recording: $isRecording")
                appendLine("- Is Playing: $isCurrentlyPlaying")
                appendLine("- Recording Duration: ${recordingDuration}ms")
                appendLine("- File Path: ${recordingFile?.absolutePath ?: "None"}")
                appendLine("- File Size: ${recordingFile?.length() ?: 0} bytes")
                appendLine("- MediaRecorder State: ${if (mediaRecorder != null) "Active" else "Inactive"}")
                appendLine("- MediaPlayer State: ${if (mediaPlayer != null) "Active" else "Inactive"}")
            }
            
            android.util.Log.d("RecordingActivity", debugSummary)
            Toast.makeText(this, "Debug info logged - check logcat for details", Toast.LENGTH_SHORT).show()
            
            // Check microphone levels and suggest noise reduction
            checkMicrophoneLevels()
            suggestNoiseReduction()
            
            // Test audio playback
            testAudioPlayback()
        }
        
        btnMonitor.setOnClickListener {
            monitorRecordingProgress()
        }
        
        btnTestSources.setOnClickListener {
            testAudioSources()
        }
        
        btnTestConfigs.setOnClickListener {
            testAudioConfigurations()
        }
    }
    
    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
            != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
            != PackageManager.PERMISSION_GRANTED) {
            
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                PERMISSION_REQUEST_CODE
            )
        }
    }
    
    private fun checkMicrophoneStatus() {
        val hasRecordPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val hasWritePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        val hasReadPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        
        val microphoneInfo = """
            Microphone Status:
            - Record Audio Permission: $hasRecordPermission
            - Write Storage Permission: $hasWritePermission
            - Read Storage Permission: $hasReadPermission
            - Package Manager: ${packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_MICROPHONE)}
        """.trimIndent()
        
        Toast.makeText(this, microphoneInfo, Toast.LENGTH_LONG).show()
    }
    
    private fun ensureAudioVolume() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        
        // If volume is too low (less than 50% of max), set it to 75%
        if (currentVolume < maxVolume * 0.5) {
            val targetVolume = (maxVolume * 0.75).toInt()
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
            Toast.makeText(this, "Audio volume adjusted to ensure playback is audible", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun debugAudioInfo() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
        
        val debugInfo = """
            Audio Debug Info:
            - Max Volume: $maxVolume
            - Current Volume: $currentVolume
            - Recording File: ${recordingFile?.absolutePath}
            - File Exists: ${recordingFile?.exists()}
            - File Size: ${recordingFile?.length()} bytes
            - Is Recording: $isRecording
            - Is Playing: $isCurrentlyPlaying
            - Audio Stream: VOICE_CALL
            - Audio Format: AMR_NB
            - Sample Rate: 8000Hz
            - Bitrate: 12.2kbps
        """.trimIndent()
        
        // Use Log instead of Toast to avoid spam
        android.util.Log.d("RecordingActivity", debugInfo)
        Toast.makeText(this, "Debug info logged - check logcat", Toast.LENGTH_SHORT).show()
    }
    
    private fun verifyRecordingFile() {
        if (recordingFile == null || !recordingFile!!.exists()) {
            Toast.makeText(this, "No recording file to verify", Toast.LENGTH_SHORT).show()
            return
        }
        
        val fileSize = recordingFile!!.length()
        val filePath = recordingFile!!.absolutePath
        
        val verificationInfo = """
            Recording File Verification:
            - File Path: $filePath
            - File Size: $fileSize bytes
            - File Exists: ${recordingFile!!.exists()}
            - Can Read: ${recordingFile!!.canRead()}
            - Duration: ${recordingDuration}ms
            - Recording Time: ${recordingDuration / 1000} seconds
        """.trimIndent()
        
        Toast.makeText(this, verificationInfo, Toast.LENGTH_LONG).show()
        
        // Check if file size is reasonable (should be > 1KB for a short recording)
        if (fileSize < 1024) {
            Toast.makeText(this, "WARNING: Recording file is very small - may not contain audio", Toast.LENGTH_LONG).show()
        } else if (fileSize > 1024 * 1024) {
            Toast.makeText(this, "Recording file is large (${fileSize / 1024}KB) - should contain audio", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun monitorRecordingProgress() {
        if (!isRecording || recordingFile == null) {
            Toast.makeText(this, "No active recording to monitor", Toast.LENGTH_SHORT).show()
            return
        }
        
        val currentTime = System.currentTimeMillis()
        val elapsedTime = currentTime - recordingStartTime
        val fileSize = recordingFile!!.length()
        
        val progressInfo = """
            Recording Progress:
            - Elapsed Time: ${elapsedTime / 1000} seconds
            - File Size: $fileSize bytes
            - Recording Rate: ${if (elapsedTime > 0) (fileSize * 1000 / elapsedTime) else 0} bytes/sec
            - File Path: ${recordingFile!!.absolutePath}
        """.trimIndent()
        
        android.util.Log.d("RecordingActivity", progressInfo)
        Toast.makeText(this, "Recording: ${elapsedTime / 1000}s, Size: ${fileSize / 1024}KB", Toast.LENGTH_SHORT).show()
    }
    
    private fun checkRecordingStatus() {
        val statusInfo = """
            Recording Status:
            - Is Recording: $isRecording
            - Is Playing: $isCurrentlyPlaying
            - Recording Duration: ${recordingDuration}ms
            - File Path: ${recordingFile?.absolutePath ?: "None"}
            - File Size: ${recordingFile?.length() ?: 0} bytes
            - MediaRecorder State: ${if (mediaRecorder != null) "Active" else "Inactive"}
            - MediaPlayer State: ${if (mediaPlayer != null) "Active" else "Inactive"}
            - Recording Start Time: ${if (recordingStartTime > 0) "Set" else "Not Set"}
        """.trimIndent()
        
        Toast.makeText(this, statusInfo, Toast.LENGTH_LONG).show()
    }
    
    private fun checkMicrophoneLevels() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
        
        val levelInfo = """
            Microphone Levels:
            - Voice Call Max Volume: $maxVolume
            - Voice Call Current Volume: $currentVolume
            - Music Max Volume: ${audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)}
            - Music Current Volume: ${audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)}
            - Microphone Gain: ${if (currentVolume > maxVolume * 0.7) "HIGH (may cause noise)" else "Normal"}
        """.trimIndent()
        
        android.util.Log.d("RecordingActivity", levelInfo)
        
        // Suggest volume adjustment if too high
        if (currentVolume > maxVolume * 0.7) {
            Toast.makeText(this, "High microphone gain detected - may cause noise", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Microphone levels appear normal", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun suggestNoiseReduction() {
        val suggestions = """
            Noise Reduction Suggestions:
            1. Use VOICE_RECOGNITION audio source (already applied)
            2. Lower bitrate (32kbps) and sampling rate (16kHz)
            3. Check device microphone gain settings
            4. Record in quiet environment
            5. Hold device closer to mouth
            6. Avoid covering microphone
        """.trimIndent()
        
        android.util.Log.d("RecordingActivity", suggestions)
        Toast.makeText(this, "Noise reduction tips logged - check logcat", Toast.LENGTH_LONG).show()
    }
    
    private fun testAudioConfigurations() {
        val configurations = listOf(
            "VOICE_RECOGNITION_16K" to Triple(MediaRecorder.AudioSource.VOICE_RECOGNITION, 16000, 32000),
            "VOICE_COMMUNICATION_22K" to Triple(MediaRecorder.AudioSource.VOICE_COMMUNICATION, 22050, 64000),
            "MIC_44K" to Triple(MediaRecorder.AudioSource.MIC, 44100, 128000),
            "DEFAULT_22K" to Triple(MediaRecorder.AudioSource.DEFAULT, 22050, 64000)
        )
        
        val testResults = mutableListOf<String>()
        
        configurations.forEach { (name, config) ->
            val (source, sampleRate, bitRate) = config
            try {
                val testRecorder = MediaRecorder()
                testRecorder.setAudioSource(source)
                testRecorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
                testRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                testRecorder.setAudioEncodingBitRate(bitRate)
                testRecorder.setAudioSamplingRate(sampleRate)
                
                val testFile = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "test_$name.aac")
                testRecorder.setOutputFile(testFile.absolutePath)
                
                testRecorder.prepare()
                testRecorder.start()
                
                // Record for 2 seconds
                Thread.sleep(2000)
                
                testRecorder.stop()
                testRecorder.release()
                
                val fileSize = testFile.length()
                val recordingRate = fileSize / 2 // bytes per second
                testResults.add("$name: ${fileSize} bytes (${recordingRate} B/s)")
                
                // Clean up test file
                testFile.delete()
                
            } catch (e: Exception) {
                testResults.add("$name: FAILED - ${e.message}")
            }
        }
        
        val resultText = "Audio Configuration Test Results:\n${testResults.joinToString("\n")}"
        android.util.Log.d("RecordingActivity", resultText)
        Toast.makeText(this, "Audio config test completed - check logcat", Toast.LENGTH_LONG).show()
        
        // Suggest the best configuration
        val bestConfig = testResults.filter { it.contains("bytes") && !it.contains("FAILED") }
            .maxByOrNull { it.split(": ")[1].split(" ")[0].toIntOrNull() ?: 0 }
        
        if (bestConfig != null) {
            Toast.makeText(this, "Best config: ${bestConfig.split(":")[0]}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun testAudioSources() {
        val audioSources = listOf(
            "MIC" to MediaRecorder.AudioSource.MIC,
            "VOICE_COMMUNICATION" to MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            "VOICE_RECOGNITION" to MediaRecorder.AudioSource.VOICE_RECOGNITION,
            "DEFAULT" to MediaRecorder.AudioSource.DEFAULT
        )
        
        val testResults = mutableListOf<String>()
        
        audioSources.forEach { (name, source) ->
            try {
                val testRecorder = MediaRecorder()
                testRecorder.setAudioSource(source)
                testRecorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
                testRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                testRecorder.setAudioEncodingBitRate(64000)
                testRecorder.setAudioSamplingRate(22050)
                
                val testFile = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "test_$name.aac")
                testRecorder.setOutputFile(testFile.absolutePath)
                
                testRecorder.prepare()
                testRecorder.start()
                
                // Record for 1 second
                Thread.sleep(1000)
                
                testRecorder.stop()
                testRecorder.release()
                
                val fileSize = testFile.length()
                testResults.add("$name: ${fileSize} bytes")
                
                // Clean up test file
                testFile.delete()
                
            } catch (e: Exception) {
                testResults.add("$name: FAILED - ${e.message}")
            }
        }
        
        val resultText = "Audio Source Test Results:\n${testResults.joinToString("\n")}"
        Toast.makeText(this, resultText, Toast.LENGTH_LONG).show()
    }
    
    private fun checkFileAccessibility() {
        if (recordingFile == null || !recordingFile!!.exists()) {
            Toast.makeText(this, "No recording file to check", Toast.LENGTH_SHORT).show()
            return
        }
        
        val file = recordingFile!!
        val fileSize = file.length()
        val canRead = file.canRead()
        val canWrite = file.canWrite()
        val lastModified = file.lastModified()
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        
        val accessibilityInfo = """
            File Accessibility:
            - File Name: ${file.name}
            - File Size: $fileSize bytes (${fileSize / 1024}KB)
            - Can Read: $canRead
            - Can Write: $canWrite
            - Last Modified: ${dateFormat.format(java.util.Date(lastModified))}
            - File Extension: ${file.extension}
            - Is Audio File: ${file.extension.lowercase() in listOf("m4a", "mp3", "wav", "aac")}
        """.trimIndent()
        
        Toast.makeText(this, accessibilityInfo, Toast.LENGTH_LONG).show()
        
        // Check if file size suggests audio content
        if (fileSize > 0) {
            val estimatedDuration = (fileSize / 16000).toInt() // Rough estimate: 16KB per second
            Toast.makeText(this, "Estimated duration: ~${estimatedDuration} seconds", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun testAudioPlayback() {
        try {
            // Create a simple test tone using ToneGenerator
            val toneGen = android.media.ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            toneGen.startTone(android.media.ToneGenerator.TONE_DTMF_1, 1000)
            
            Toast.makeText(this, "Playing test tone - you should hear a beep", Toast.LENGTH_SHORT).show()
            
            // Clean up after 1 second
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                toneGen.release()
            }, 1000)
        } catch (e: Exception) {
            Toast.makeText(this, "Error playing test tone: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun startRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Recording permission required", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Clean up any existing MediaRecorder first
        cleanupMediaRecorder()
        
        try {
            // Create output file
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "SpeakEasy_Recording_$timeStamp.3gp"
            
            val recordingsDir = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "SpeakEasy")
            if (!recordingsDir.exists()) {
                recordingsDir.mkdirs()
            }
            
            recordingFile = File(recordingsDir, fileName)
            outputFile = recordingFile?.absolutePath
            
            // Initialize MediaRecorder with noise reduction settings
            @Suppress("DEPRECATION")
            mediaRecorder = MediaRecorder().apply {
                // Use most basic settings for maximum stability
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP) // Most compatible format
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB) // Use AMR encoder for voice
                setAudioEncodingBitRate(12200) // AMR standard bitrate
                setAudioSamplingRate(8000) // AMR standard sampling rate
                setOutputFile(outputFile)
                
                try {
                    prepare()
                    start()
                    
                    isRecording = true
                    recordingStartTime = System.currentTimeMillis()
                    updateUI()
                    startTimer()
                    
                    android.util.Log.d("RecordingActivity", "Recording started with: MIC source, 8kHz, 12.2kbps AMR")
                    Toast.makeText(this@RecordingActivity, "Recording started successfully", Toast.LENGTH_SHORT).show()
                } catch (e: IOException) {
                    val errorMsg = "Failed to start recording: ${e.message}"
                    Toast.makeText(this@RecordingActivity, errorMsg, Toast.LENGTH_LONG).show()
                    android.util.Log.e("RecordingActivity", errorMsg, e)
                    // Clean up on error
                    cleanupMediaRecorder()
                } catch (e: IllegalStateException) {
                    val errorMsg = "Recorder in illegal state: ${e.message}"
                    Toast.makeText(this@RecordingActivity, errorMsg, Toast.LENGTH_LONG).show()
                    android.util.Log.e("RecordingActivity", errorMsg, e)
                    // Clean up on error
                    cleanupMediaRecorder()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing recorder: ${e.message}", Toast.LENGTH_SHORT).show()
            // Clean up on error
            cleanupMediaRecorder()
        }
    }
    
    private fun cleanupMediaRecorder() {
        mediaRecorder?.apply {
            try {
                if (isRecording) {
                    android.util.Log.d("RecordingActivity", "Stopping MediaRecorder")
                    stop()
                }
            } catch (e: Exception) {
                android.util.Log.w("RecordingActivity", "Error stopping MediaRecorder: ${e.message}")
            }
            try {
                android.util.Log.d("RecordingActivity", "Resetting MediaRecorder")
                reset()
            } catch (e: Exception) {
                android.util.Log.w("RecordingActivity", "Error resetting MediaRecorder: ${e.message}")
            }
            try {
                android.util.Log.d("RecordingActivity", "Releasing MediaRecorder")
                release()
            } catch (e: Exception) {
                android.util.Log.w("RecordingActivity", "Error releasing MediaRecorder: ${e.message}")
            }
        }
        mediaRecorder = null
        isRecording = false
        android.util.Log.d("RecordingActivity", "MediaRecorder cleanup completed")
    }
    
    private fun stopRecording() {
        try {
            cleanupMediaRecorder()
            
            recordingDuration = System.currentTimeMillis() - recordingStartTime
            updateUI()
            stopTimer()
            
            // Verify the recording file was created and has content
            if (recordingFile != null && recordingFile!!.exists()) {
                val fileSize = recordingFile!!.length()
                if (fileSize > 0) {
                    Toast.makeText(this, "Recording stopped - File size: ${fileSize} bytes", Toast.LENGTH_SHORT).show()
                    // Enable play and save buttons
                    btnPlay.isEnabled = true
                    btnSave.isEnabled = true
                } else {
                    Toast.makeText(this, "Warning: Recording file is empty", Toast.LENGTH_LONG).show()
                    btnPlay.isEnabled = false
                    btnSave.isEnabled = false
                }
            } else {
                Toast.makeText(this, "Error: Recording file not found", Toast.LENGTH_LONG).show()
                btnPlay.isEnabled = false
                btnSave.isEnabled = false
            }
        } catch (e: Exception) {
            val errorMsg = "Error stopping recording: ${e.message}"
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            android.util.Log.e("RecordingActivity", errorMsg, e)
        }
    }
    
    private fun startPlaying() {
        if (recordingFile == null || !recordingFile!!.exists()) {
            Toast.makeText(this, "No recording file found at: ${recordingFile?.absolutePath}", Toast.LENGTH_LONG).show()
            return
        }
        
        // Validate the recording file
        val fileSize = recordingFile!!.length()
        if (fileSize == 0L) {
            Toast.makeText(this, "Recording file is empty", Toast.LENGTH_SHORT).show()
            return
        }
        
        android.util.Log.d("RecordingActivity", "Playing recording: ${recordingFile!!.absolutePath}, size: ${fileSize} bytes")
        
        // Debug: Show file info
        Toast.makeText(this, "Attempting to play: ${recordingFile!!.absolutePath}", Toast.LENGTH_SHORT).show()
        
        try {
            // Stop any currently playing audio first
            stopPlaying()
            
            // Request audio focus with simplified approach to avoid attribution issues
            val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            var audioFocusGranted = false
            
            try {
                // Use a simpler audio focus request that doesn't trigger attribution issues
                val result = audioManager.requestAudioFocus(
                    object : AudioManager.OnAudioFocusChangeListener {
                        override fun onAudioFocusChange(focusChange: Int) {
                            when (focusChange) {
                                AudioManager.AUDIOFOCUS_LOSS -> {
                                    stopPlaying()
                                }
                                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                                    // Don't pause, just continue playing
                                    android.util.Log.d("RecordingActivity", "Audio focus lost temporarily")
                                }
                                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                                    // Reduce volume instead of stopping
                                    mediaPlayer?.setVolume(0.1f, 0.1f)
                                    android.util.Log.d("RecordingActivity", "Audio focus lost - reducing volume")
                                }
                                AudioManager.AUDIOFOCUS_GAIN -> {
                                    // Restore to normal volume (30%) instead of maximum
                                    mediaPlayer?.setVolume(0.3f, 0.3f)
                                    android.util.Log.d("RecordingActivity", "Audio focus gained - restoring volume")
                                }
                            }
                        }
                    },
                    AudioManager.STREAM_VOICE_CALL,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
                
                audioFocusGranted = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            } catch (e: Exception) {
                // If audio focus request fails, continue anyway
                audioFocusGranted = false
                android.util.Log.w("RecordingActivity", "Audio focus request failed: ${e.message}")
            }
            
            if (!audioFocusGranted) {
                Toast.makeText(this, "Audio focus not granted, but attempting playback anyway", Toast.LENGTH_SHORT).show()
            }
            
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
                    
                    setDataSource(recordingFile!!.absolutePath)
                    prepare()
                    
                    // Set volume to a reasonable level (30% instead of 100%)
                    setVolume(0.3f, 0.3f)
                    
                    start()
                    
                    isCurrentlyPlaying = true
                    updateUI()
                    
                    Toast.makeText(this@RecordingActivity, "Playing recording successfully", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    val errorMsg = "Error starting playback: ${e.message}"
                    Toast.makeText(this@RecordingActivity, errorMsg, Toast.LENGTH_LONG).show()
                    android.util.Log.e("RecordingActivity", errorMsg, e)
                    isCurrentlyPlaying = false
                    updateUI()
                    // Clean up on error
                    reset()
                    release()
                    mediaPlayer = null
                }
                
                setOnCompletionListener {
                    isCurrentlyPlaying = false
                    updateUI()
                    // Abandon audio focus
                    try {
                        audioManager.abandonAudioFocus(null)
                    } catch (e: Exception) {
                        android.util.Log.w("RecordingActivity", "Error abandoning audio focus: ${e.message}")
                    }
                }
                
                setOnErrorListener { _, what, extra ->
                    val errorMsg = "Playback error: $what"
                    Toast.makeText(this@RecordingActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    android.util.Log.e("RecordingActivity", errorMsg)
                    isCurrentlyPlaying = false
                    updateUI()
                    // Abandon audio focus
                    try {
                        audioManager.abandonAudioFocus(null)
                    } catch (e: Exception) {
                        android.util.Log.w("RecordingActivity", "Error abandoning audio focus: ${e.message}")
                    }
                    true
                }
            }
        } catch (e: Exception) {
            val errorMsg = "Error playing recording: ${e.message}"
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            android.util.Log.e("RecordingActivity", errorMsg, e)
        }
    }
    
    private fun stopPlaying() {
        try {
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
            
            isCurrentlyPlaying = false
            updateUI()
            
            Toast.makeText(this, "Playback stopped", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error stopping playback", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun saveRecording() {
        if (recordingFile == null || !recordingFile!!.exists()) {
            Toast.makeText(this, "No recording to save", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Show dialog to get recording title
        val input = android.widget.EditText(this)
        input.hint = "Enter recording title"
        
        AlertDialog.Builder(this)
            .setTitle("Save Recording")
            .setMessage("Enter a title for your recording:")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val title = input.text.toString().trim()
                if (title.isNotEmpty()) {
                    saveRecordingToDatabase(title)
                } else {
                    Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun saveRecordingToDatabase(title: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val promptTitle = intent.getStringExtra("prompt_title")
                val promptContent = intent.getStringExtra("prompt_content")
                val scriptContent = intent.getStringExtra("script_content")
                
                val prompt = when {
                    promptTitle != null && promptContent != null -> "$promptTitle: $promptContent"
                    scriptContent != null -> "Text Script: $scriptContent"
                    else -> null
                }
                
                val recording = Recording(
                    userId = getCurrentUserId(),
                    fileName = recordingFile!!.name,
                    filePath = recordingFile!!.absolutePath,
                    title = title,
                    duration = recordingDuration,
                    fileSize = recordingFile!!.length(),
                    prompt = prompt
                )
                
                val recordingId = recordingDao.insertRecording(recording)
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RecordingActivity, "Recording saved successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RecordingActivity, "Error saving recording: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun getCurrentUserId(): String {
        val sharedPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        return sharedPrefs.getString("user_name", "unknown") ?: "unknown"
    }
    
    private fun cancelRecording() {
        // Delete the recording file if it exists
        recordingFile?.let { file ->
            if (file.exists()) {
                file.delete()
            }
        }
        
        // Stop recording if active
        if (isRecording) {
            stopRecording()
        }
        
        // Stop playback if active
        if (isCurrentlyPlaying) {
            stopPlaying()
        }
        
        Toast.makeText(this, "Recording cancelled", Toast.LENGTH_SHORT).show()
        finish()
    }
    
    private fun updateUI() {
        if (isRecording) {
            btnRecord.text = "Stop Recording"
            btnRecord.setBackgroundResource(R.drawable.rounded_button)
            tvStatus.text = "Recording in progress..."
        } else {
            btnRecord.text = "Start Recording"
            btnRecord.setBackgroundResource(R.drawable.rounded_button_outline)
            tvStatus.text = "Ready to record"
        }
        
        if (isCurrentlyPlaying) {
            btnPlay.text = "Stop Playing"
            btnPlay.setBackgroundResource(R.drawable.rounded_button)
        } else {
            btnPlay.text = "Play Recording"
            btnPlay.setBackgroundResource(R.drawable.rounded_button_outline)
        }
    }
    
    private fun startTimer() {
        timerHandler = android.os.Handler(android.os.Looper.getMainLooper())
        timerRunnable = object : Runnable {
            override fun run() {
                val elapsedTime = System.currentTimeMillis() - recordingStartTime
                val seconds = (elapsedTime / 1000).toInt()
                val minutes = seconds / 60
                val remainingSeconds = seconds % 60
                
                val timeString = String.format("%02d:%02d", minutes, remainingSeconds)
                tvTimer.text = timeString
                
                // Continue updating every second
                timerHandler?.postDelayed(this, 1000)
            }
        }
        timerHandler?.post(timerRunnable!!)
    }
    
    private fun stopTimer() {
        timerHandler?.removeCallbacks(timerRunnable!!)
        timerHandler = null
        timerRunnable = null
        tvTimer.text = "00:00"
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permissions required for recording", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (isRecording) {
            stopRecording()
        }
        if (isCurrentlyPlaying) {
            stopPlaying()
        }
        stopTimer()
        
        // Clean up MediaRecorder
        mediaRecorder?.apply {
            try {
                stop()
            } catch (e: Exception) {
                // Ignore if already stopped
            }
            reset()
            release()
        }
        mediaRecorder = null
        
        // Clean up MediaPlayer
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

    private fun checkAudioProcessingIssues() {
        try {
            val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            
            // Check if audio processing is enabled
            val isAudioProcessingEnabled = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                audioManager.isAudioProcessingEnabled
            } else {
                "Unknown (API < 23)"
            }
            
            // Check audio mode
            val audioMode = when (audioManager.mode) {
                AudioManager.MODE_NORMAL -> "NORMAL"
                AudioManager.MODE_RINGTONE -> "RINGTONE"
                AudioManager.MODE_IN_CALL -> "IN_CALL"
                AudioManager.MODE_IN_COMMUNICATION -> "IN_COMMUNICATION"
                else -> "UNKNOWN"
            }
            
            val audioInfo = """
                Audio Processing Check:
                - Audio Mode: $audioMode
                - Audio Processing Enabled: $isAudioProcessingEnabled
                - Current Stream: VOICE_CALL
                - Recording Format: THREE_GPP (3GP)
                - Encoder: AMR_NB
                - Sample Rate: 8000Hz
                - Bitrate: 12.2kbps
                - Audio Source: MIC
            """.trimIndent()
            
            android.util.Log.d("RecordingActivity", audioInfo)
            Toast.makeText(this, "Audio processing info logged", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            android.util.Log.e("RecordingActivity", "Error checking audio processing: ${e.message}")
        }
    }
} 
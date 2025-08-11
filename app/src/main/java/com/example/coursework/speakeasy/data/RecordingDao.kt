package com.example.coursework.speakeasy.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    
    @Query("SELECT * FROM recordings WHERE userId = :userId ORDER BY createdAt DESC")
    fun getRecordingsByUser(userId: String): List<Recording>
    
    @Query("SELECT * FROM recordings WHERE id = :recordingId")
    fun getRecordingById(recordingId: Long): Recording?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRecording(recording: Recording): Long
    
    @Update
    fun updateRecording(recording: Recording)
    
    @Delete
    fun deleteRecording(recording: Recording)
    
    @Query("DELETE FROM recordings WHERE id = :recordingId")
    fun deleteRecordingById(recordingId: Long)
    
    @Query("SELECT COUNT(*) FROM recordings WHERE userId = :userId")
    fun getRecordingCountForUser(userId: String): Int
    
    @Query("SELECT * FROM recordings WHERE userId = :userId AND title LIKE '%' || :searchQuery || '%' ORDER BY createdAt DESC")
    fun searchRecordings(userId: String, searchQuery: String): List<Recording>
    
    @Query("SELECT * FROM recordings WHERE userId = :userId AND score IS NOT NULL ORDER BY score DESC LIMIT 5")
    fun getTopRecordingsByScore(userId: String): List<Recording>
    
    @Query("SELECT * FROM recordings WHERE userId = :userId ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentRecordings(userId: String, limit: Int): List<Recording>
} 
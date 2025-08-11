package com.example.coursework.speakeasy.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PromptDao {
    
    @Query("SELECT * FROM prompts WHERE isCustom = 0 ORDER BY RANDOM() LIMIT 1")
    fun getRandomSystemPrompt(): Prompt?
    
    @Query("SELECT * FROM prompts WHERE isCustom = 0 ORDER BY title ASC")
    fun getAllSystemPrompts(): List<Prompt>
    
    @Query("SELECT * FROM prompts WHERE createdBy = :username ORDER BY createdAt DESC")
    fun getCustomPromptsByUser(username: String): List<Prompt>
    
    @Query("SELECT * FROM prompts WHERE category = :category AND isCustom = 0 ORDER BY RANDOM() LIMIT 1")
    fun getRandomPromptByCategory(category: String): Prompt?
    
    @Query("SELECT * FROM prompts WHERE difficulty = :difficulty AND isCustom = 0 ORDER BY RANDOM() LIMIT 1")
    fun getRandomPromptByDifficulty(difficulty: String): Prompt?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPrompt(prompt: Prompt): Long
    
    @Update
    fun updatePrompt(prompt: Prompt)
    
    @Delete
    fun deletePrompt(prompt: Prompt)
    
    @Query("DELETE FROM prompts WHERE id = :promptId")
    fun deletePromptById(promptId: Long)
    
    @Query("SELECT DISTINCT category FROM prompts WHERE isCustom = 0 ORDER BY category ASC")
    fun getAllCategories(): List<String>
    
    @Query("SELECT * FROM prompts WHERE title LIKE '%' || :searchQuery || '%' OR content LIKE '%' || :searchQuery || '%' ORDER BY title ASC")
    fun searchPrompts(searchQuery: String): List<Prompt>
} 
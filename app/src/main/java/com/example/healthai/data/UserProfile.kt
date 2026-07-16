package com.example.healthai.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * 用户身体档案。整张表只保存一条记录（id 固定为 1），方便随时覆盖更新。
 */
@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val heightCm: Float = 0f,
    val weightKg: Float = 0f,
    val age: Int = 0,
    val gender: String = "",          // "male" | "female"
    val goal: String = "",            // "lose" | "maintain" | "gain"
    val activity: String = ""         // "low" | "mid" | "high"
) {
    fun isEmpty(): Boolean =
        heightCm <= 0 || weightKg <= 0 || gender.isBlank() || goal.isBlank()

    /** 估算 BMI */
    fun bmi(): Float? {
        if (heightCm <= 0 || weightKg <= 0) return null
        val m = heightCm / 100f
        return weightKg / (m * m)
    }
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun get(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfile)
}

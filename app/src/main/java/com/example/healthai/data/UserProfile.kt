package com.example.healthai.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update

/**
 * 用户身体档案（支持多人建档）。
 * 每个档案占一行，id 自增；name 允许为空（UI 显示占位"未命名档案"），允许重名。
 */
@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",                 // 档案名，允许空（占位"未命名档案"）、允许重名
    val heightCm: Float = 0f,
    val weightKg: Float = 0f,
    val age: Int = 0,
    val gender: String = "",               // "male" | "female"
    val goal: String = "",                 // "lose" | "maintain" | "gain"
    val activity: String = ""              // "low" | "mid" | "high"
) {
    /** 关键资料是否缺失（用于判断是否需要档案才能给出精准建议） */
    fun isEmpty(): Boolean =
        heightCm <= 0 || weightKg <= 0 || gender.isBlank() || goal.isBlank()

    /** 估算 BMI，资料不完整时返回 null */
    fun bmi(): Float? {
        if (heightCm <= 0 || weightKg <= 0) return null
        val m = heightCm / 100f
        return weightKg / (m * m)
    }
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile ORDER BY id ASC")
    suspend fun getAll(): List<UserProfile>

    @Query("SELECT * FROM user_profile WHERE id = :id")
    suspend fun getById(id: Long): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(p: UserProfile): Long

    @Update
    suspend fun update(p: UserProfile)

    @Delete
    suspend fun delete(p: UserProfile)

    @Query("SELECT COUNT(*) FROM user_profile")
    suspend fun count(): Int
}

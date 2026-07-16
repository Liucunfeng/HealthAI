package com.example.healthai.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * 一次分析的归档记录。
 * @param type      "body" 身材分析 / "food" 食物分析
 * @param imageBase64 缩略图（base64，便于在历史列表里直接预览）
 * @param summary   一句话摘要
 * @param detailJson 完整结构化结果（JSON 字符串），点击后可展开查看
 */
@Entity(tableName = "analysis_records")
data class AnalysisRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val createdAt: Long,
    val imageBase64: String = "",
    val summary: String = "",
    val detailJson: String = ""
)

@Dao
interface AnalysisRecordDao {
    @Query("SELECT * FROM analysis_records ORDER BY createdAt DESC")
    suspend fun getAll(): List<AnalysisRecord>

    @Query("SELECT * FROM analysis_records WHERE type = :type ORDER BY createdAt DESC")
    suspend fun getByType(type: String): List<AnalysisRecord>

    @Insert
    suspend fun insert(record: AnalysisRecord): Long

    @Query("DELETE FROM analysis_records")
    suspend fun clear()
}

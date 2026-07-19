package com.example.healthai.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * 一次分析的归档记录。
 * @param type         "body" 身材分析 / "food" 食物分析
 * @param imageBase64  首图缩略图（base64，历史列表回退预览用；多图时取首图）
 * @param imageListJson 全部图片（最多 4 张）的 base64 列表，以 JSON 数组字符串存储；
 *                      历史页据此渲染 4 个小缩略图，不足 4 张用灰色色块占位。
 * @param summary      一句话摘要
 * @param detailJson   完整结构化结果（JSON 字符串），旧记录回退时使用
 * @param displayText  可读中文结果（新记录写入；为空时历史页回退 ResultFormatter）
 * @param profileName  分析时选中的身体档案人名（"通用建议"或空表示未指定），便于历史回顾
 */
@Entity(tableName = "analysis_records")
data class AnalysisRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val createdAt: Long,
    val imageBase64: String = "",
    val imageListJson: String = "",
    val summary: String = "",
    val detailJson: String = "",
    val displayText: String = "",
    val profileName: String = ""
)

@Dao
interface AnalysisRecordDao {
    @Query("SELECT * FROM analysis_records ORDER BY createdAt DESC")
    suspend fun getAll(): List<AnalysisRecord>

    @Query("SELECT * FROM analysis_records WHERE type = :type ORDER BY createdAt DESC")
    suspend fun getByType(type: String): List<AnalysisRecord>

    @Insert
    suspend fun insert(record: AnalysisRecord): Long

    @Query("DELETE FROM analysis_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM analysis_records")
    suspend fun clear()
}

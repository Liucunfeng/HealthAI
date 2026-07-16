package com.example.healthai.vision

import com.example.healthai.data.AppPreferences
import com.example.healthai.data.UserProfile
import android.content.Context

/**
 * 视觉分析的统一接口。身材分析与食物分析都通过它完成，
 * 上层 UI 不关心背后是 OpenAI、腾讯云还是端侧模型——
 * 想换服务商，只新增一个实现类即可。
 */
interface VisionAnalyzer {
    suspend fun analyzeBody(imageBase64: String, profile: UserProfile?): BodyAnalysis
    suspend fun analyzeFood(imageBase64: String, profile: UserProfile?): FoodAnalysis
}

/**
 * 根据本地保存的设置创建分析器。
 * 未配置 API Key 时返回 null，调用方应提示用户先去「设置」页填写。
 */
object VisionAnalyzerFactory {
    fun create(context: Context): VisionAnalyzer? {
        val key = AppPreferences.getApiKey(context)
        if (key.isBlank()) return null
        val base = AppPreferences.getApiBase(context)
        val model = AppPreferences.getModel(context)
        return OpenAIVisionAnalyzer(apiKey = key, baseUrl = base, model = model)
    }
}

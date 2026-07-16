package com.example.healthai.vision

import com.example.healthai.data.UserProfile

/**
 * 视觉分析的统一接口。身材分析与食物分析都通过它完成，
 * 上层 UI 不关心背后是 OpenAI、腾讯云还是端侧模型——
 * 想换服务商，只新增一个实现类即可。
 */
interface VisionAnalyzer {
    suspend fun analyzeBody(imageBase64: String, profile: UserProfile?): BodyAnalysis
    suspend fun analyzeFood(imageBase64: String, profile: UserProfile?): FoodAnalysis
}

package com.example.healthai.vision

import com.example.healthai.data.UserProfile

/**
 * 视觉分析的统一接口。身材分析与食物分析都通过它完成，
 * 上层 UI 不关心背后是 OpenAI、腾讯云还是端侧模型——
 * 想换服务商，只新增一个实现类即可。
 *
 * R1 多图分析：analyzeBody / analyzeFood 接受图片列表（最多 4 张）联合分析。
 */
interface VisionAnalyzer {
    suspend fun analyzeBody(images: List<String>, profile: UserProfile?): BodyAnalysis
    suspend fun analyzeFood(images: List<String>, profile: UserProfile?): FoodAnalysis
}

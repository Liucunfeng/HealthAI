package com.example.healthai.vision

/** 身材分析 / 食物分析的结构化结果模型。字段均有默认值，便于容错解析。 */

data class Metric(
    val label: String = "",
    val value: String = "",
    val comment: String = ""
)

data class BodyAnalysis(
    val overall: String = "",
    val bodyType: String = "",
    val proportions: List<Metric> = emptyList(),
    val physique: String = "",
    val bodyFatEstimate: String = "",
    val advice: List<String> = emptyList()
)

data class FoodItem(
    val name: String = "",
    val caloriesKcal: Int = 0,
    val proteinG: Float = 0f,
    val carbG: Float = 0f,
    val fatG: Float = 0f,
    val portion: String = ""
)

data class FoodAnalysis(
    val foods: List<FoodItem> = emptyList(),
    val totalCalories: Int = 0,
    val totalProteinG: Float = 0f,
    val totalCarbG: Float = 0f,
    val totalFatG: Float = 0f,
    val suitable: String = "",          // "suitable" | "moderate" | "unsuitable"
    val reason: String = "",
    val adjustment: String = ""
)

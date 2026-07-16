package com.example.healthai.util

import com.example.healthai.data.UserProfile

/**
 * 构造发给视觉大模型的提示词。
 * 关键约束：要求模型只返回纯 JSON（response_format=json_object），
 * 字段严格对应 vision/models.kt 中的结构体，方便后续解析。
 */
object PromptBuilder {

    private val goalText = mapOf("lose" to "减脂", "maintain" to "维持体重", "gain" to "增肌")
    private val activityText = mapOf("low" to "久坐少动", "mid" to "中等活动量", "high" to "高强度活动")

    fun profileBlock(profile: UserProfile?): String {
        if (profile == null || profile.isEmpty()) {
            return "用户暂未提供身体档案，请基于照片给出通用建议。"
        }
        val bmi = profile.bmi()?.let { "BMI 约 %.1f" % it } ?: ""
        return """
            用户身体档案：
            - 性别：${if (profile.gender == "male") "男" else "女"}
            - 年龄：${profile.age}
            - 身高：${profile.heightCm} cm
            - 体重：${profile.weightKg} kg
            - $bmi
            - 健康目标：${goalText[profile.goal] ?: profile.goal}
            - 活动水平：${activityText[profile.activity] ?: profile.activity}
        """.trimIndent()
    }

    fun bodyPrompt(profile: UserProfile?): String = """
        你是一位专业的体姿与体质评估专家。请根据这张照片评估人物的身材比例与体质。
        ${profileBlock(profile)}

        请只返回一个 JSON 对象，不要包含任何额外文字、不要使用 markdown 代码块。结构如下：
        {
          "overall": "一句话总体评价",
          "bodyType": "体型分类，例如苹果型/梨型/匀称型/肌肉型/偏瘦型等",
          "proportions": [
            {"label": "比例项名称（如腿身比、肩腰比）", "value": "数值或比例", "comment": "简短解读"}
          ],
          "physique": "体质描述（肌肉量、线条、体态、对称性等）",
          "bodyFatEstimate": "体脂率估算，如约 18%",
          "advice": ["建议1", "建议2", "建议3"]
        }
    """.trimIndent()

    fun foodPrompt(profile: UserProfile?): String = """
        你是一位注册营养师。请识别这张照片中的食物，估算每种食物的营养与热量，
        并结合用户档案判断这道/这餐食物是否适合吃。
        ${profileBlock(profile)}

        请只返回一个 JSON 对象，不要包含任何额外文字、不要使用 markdown 代码块。结构如下：
        {
          "foods": [
            {"name": "食物名", "caloriesKcal": 200, "proteinG": 4, "carbG": 44, "fatG": 0.5, "portion": "约1碗"}
          ],
          "totalCalories": 520,
          "totalProteinG": 28,
          "totalCarbG": 70,
          "totalFatG": 12,
          "suitable": "suitable 或 moderate 或 unsuitable",
          "reason": "结合用户目标与体质，说明适合/不适合的原因",
          "adjustment": "如果不合适或需要优化，给出具体调整建议（替换、减量、搭配、进餐顺序等）"
        }
    """.trimIndent()
}

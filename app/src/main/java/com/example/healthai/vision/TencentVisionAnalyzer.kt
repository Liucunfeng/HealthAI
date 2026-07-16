package com.example.healthai.vision

import com.example.healthai.data.UserProfile

/**
 * 腾讯云视觉分析适配器（模板 / 占位）。
 *
 * 腾讯云没有与 OpenAI 完全对等的「通用多模态对话」公开接口，它提供的是细分能力
 * （图像标签、商品识别、人体分析、OCR 等）。若要接入，常见两条路：
 *
 * 1) 腾讯混元大模型（Hunyuan）多模态对话：
 *    若已开通混元多模态接口，直接参照 OpenAIVisionAnalyzer 的结构，把请求体改为
 *    混元文档要求的 chat/completions 格式，并把 base64 图片按混元规范塞入 content
 *    数组即可，解析逻辑（parseBody / parseFood）完全可复用。
 *
 * 2) 腾讯云 AI 原子能力组合（无需大模型）：
 *    - 用「人体分析」关键点接口估算身材比例；
 *    - 用「图像标签 / 商品识别」拿到食物类别；
 *    - 再用本地营养表换算热量与宏量营养。
 *    这条路不需要大模型，但工程量大、识别范围有限。
 *
 * 这里保留接口契约，默认抛出未实现异常，方便你后续填充真实逻辑。
 */
class TencentVisionAnalyzer : VisionAnalyzer {
    override suspend fun analyzeBody(imageBase64: String, profile: UserProfile?): BodyAnalysis {
        throw UnsupportedOperationException(
            "腾讯云视觉适配器尚未实现，请参考 TencentVisionAnalyzer.kt 顶部注释接入。"
        )
    }

    override suspend fun analyzeFood(imageBase64: String, profile: UserProfile?): FoodAnalysis {
        throw UnsupportedOperationException(
            "腾讯云视觉适配器尚未实现，请参考 TencentVisionAnalyzer.kt 顶部注释接入。"
        )
    }
}

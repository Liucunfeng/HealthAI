package com.example.healthai.vision

import com.example.healthai.data.AppPreferences
import android.content.Context

/**
 * 分析器工厂：根据设置页的配置产出 VisionAnalyzer 实例。
 *
 * 默认返回 [OpenAIVisionAnalyzer]（OpenAI 兼容视觉接口）。
 * 未配置 API Key 时返回 null，调用方（Fragment）应提示用户先去「设置」页填写。
 * 若要切换到腾讯云 / 混元视觉，把下面 return 那一行换成：
 *     return TencentVisionAnalyzer()
 * 上层 UI（Fragment）无需任何改动。
 */
object VisionAnalyzerFactory {

    fun create(context: Context): VisionAnalyzer? {
        val apiKey = AppPreferences.getApiKey(context)
        if (apiKey.isBlank()) return null
        val baseUrl = AppPreferences.getApiBase(context)
        val model = AppPreferences.getModel(context)
        return OpenAIVisionAnalyzer(apiKey, baseUrl, model)
    }
}

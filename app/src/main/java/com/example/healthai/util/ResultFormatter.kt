package com.example.healthai.util

import com.example.healthai.data.AnalysisRecord
import com.example.healthai.vision.BodyAnalysis
import com.example.healthai.vision.FoodAnalysis
import com.example.healthai.vision.FoodItem
import com.example.healthai.vision.Metric
import com.google.gson.Gson
import com.google.gson.JsonObject

/**
 * 共享的结果格式化工具（R4 历史净化的根因治理）。
 *
 * 把身材 / 食物分析结果统一格式化为可读中文，供：
 * - 分析页结果展示（tvResult / tvFoodResult）
 * - 历史卡片（HistoryAdapter）
 * - 历史详情（HistoryFragment.showDetail）
 * 三处统一调用，保证格式一致，并彻底去除原始 JSON 的 `{} : " ` 与英文键。
 *
 * 旧记录（displayText 为空）通过 formatFromDetail 用 Gson 解析 detailJson 回退。
 */
object ResultFormatter {

    /** 身材结果 → 可读中文（提取自原 BodyAnalysisFragment.showResult） */
    fun formatBody(r: BodyAnalysis): String {
        val sb = StringBuilder()
        sb.appendLine("总体评价：${r.overall}")
        if (r.bodyType.isNotBlank()) sb.appendLine("体型：${r.bodyType}")
        if (r.proportions.isNotEmpty()) {
            sb.appendLine("\n比例指标：")
            r.proportions.forEach { sb.appendLine("· ${it.label}：${it.value} —— ${it.comment}") }
        }
        if (r.physique.isNotBlank()) sb.appendLine("\n体质：${r.physique}")
        if (r.bodyFatEstimate.isNotBlank()) sb.appendLine("体脂估算：${r.bodyFatEstimate}")
        if (r.advice.isNotEmpty()) {
            sb.appendLine("\n建议：")
            r.advice.forEach { sb.appendLine("· $it") }
        }
        return sb.toString().trimEnd()
    }

    /** 食物结果 → 可读中文（提取自原 FoodAnalysisFragment.showResult） */
    fun formatFood(r: FoodAnalysis): String {
        val sb = StringBuilder()
        if (r.foods.isNotEmpty()) {
            sb.appendLine("识别到的食物：")
            r.foods.forEach { f ->
                sb.appendLine("· ${f.name}（${f.portion}） 约 ${f.caloriesKcal} kcal")
                sb.appendLine("   蛋白 ${f.proteinG}g / 碳水 ${f.carbG}g / 脂肪 ${f.fatG}g")
            }
        }
        sb.appendLine(
            "\n合计：约 ${r.totalCalories} kcal | 蛋白 ${r.totalProteinG}g | 碳水 ${r.totalCarbG}g | 脂肪 ${r.totalFatG}g"
        )
        val suitableText = when (r.suitable) {
            "suitable" -> "适合"
            "moderate" -> "适量即可"
            "unsuitable" -> "不太适合"
            else -> r.suitable
        }
        sb.appendLine("\n是否适合你：$suitableText")
        if (r.reason.isNotBlank()) sb.appendLine("原因：${r.reason}")
        if (r.adjustment.isNotBlank()) sb.appendLine("\n调整建议：${r.adjustment}")
        return sb.toString().trimEnd()
    }

    /** 旧记录回退：用 Gson 解析 detailJson → BodyAnalysis/FoodAnalysis → 对应 format */
    fun formatFromDetail(type: String, detailJson: String): String {
        if (detailJson.isBlank()) return ""
        val root = runCatching { Gson().fromJson(detailJson, JsonObject::class.java) }
            .getOrNull() ?: return ""
        return if (type == "body") formatBody(parseBody(root)) else formatFood(parseFood(root))
    }

    /** 统一入口：displayText 非空直接返回，否则回退 formatFromDetail */
    fun displayTextFor(rec: AnalysisRecord): String =
        rec.displayText.ifBlank { formatFromDetail(rec.type, rec.detailJson) }

    // —— 以下解析逻辑与原 OpenAIVisionAnalyzer.parseBody/parseFood 一致，保证回退结果一致 ——

    private fun parseBody(root: JsonObject): BodyAnalysis {
        val proportions = root.optArray("proportions").mapNotNull { el ->
            if (!el.isJsonObject) null
            else {
                val m = el.asJsonObject
                Metric(m.optString("label"), m.optString("value"), m.optString("comment"))
            }
        }
        val advice = root.optArray("advice").mapNotNull { el ->
            if (el.isJsonPrimitive && el.asJsonPrimitive.isString) el.asString else null
        }
        return BodyAnalysis(
            overall = root.optString("overall"),
            bodyType = root.optString("bodyType"),
            proportions = proportions,
            physique = root.optString("physique"),
            bodyFatEstimate = root.optString("bodyFatEstimate"),
            advice = advice
        )
    }

    private fun parseFood(root: JsonObject): FoodAnalysis {
        val foods = root.optArray("foods").mapNotNull { el ->
            if (!el.isJsonObject) null
            else {
                val f = el.asJsonObject
                FoodItem(
                    name = f.optString("name"),
                    caloriesKcal = f.optInt("caloriesKcal"),
                    proteinG = f.optFloat("proteinG"),
                    carbG = f.optFloat("carbG"),
                    fatG = f.optFloat("fatG"),
                    portion = f.optString("portion")
                )
            }
        }
        return FoodAnalysis(
            foods = foods,
            totalCalories = root.optInt("totalCalories"),
            totalProteinG = root.optFloat("totalProteinG"),
            totalCarbG = root.optFloat("totalCarbG"),
            totalFatG = root.optFloat("totalFatG"),
            suitable = root.optString("suitable"),
            reason = root.optString("reason"),
            adjustment = root.optString("adjustment")
        )
    }
}

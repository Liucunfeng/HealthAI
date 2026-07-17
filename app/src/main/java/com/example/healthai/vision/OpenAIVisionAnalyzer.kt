package com.example.healthai.vision

import com.example.healthai.data.UserProfile
import com.example.healthai.util.PromptBuilder
import com.example.healthai.util.optArray
import com.example.healthai.util.optFloat
import com.example.healthai.util.optInt
import com.example.healthai.util.optString
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

/**
 * 基于 OpenAI 兼容「视觉对话」接口的实现。
 * 只要是 OpenAI 兼容服务（如 OpenAI 官方、Azure OpenAI、或自托管兼容网关）
 * 都能直接用——改 baseUrl / model 即可。
 *
 * R1 多图分析：analyzeBody / analyzeFood 接受图片列表；callVision 为每张图
 * 各拼一个 image_url content block，text block 仅 1 个（在首部）。
 */
class OpenAIVisionAnalyzer(
    private val apiKey: String,
    baseUrl: String,
    private val model: String
) : VisionAnalyzer {

    private val endpoint = run {
        var raw = if (baseUrl.isBlank()) {
            "https://api.openai.com/v1"
        } else {
            baseUrl.trim().trimEnd('/')
        }
        // 容错 1：用户可能把完整接口地址 https://api.moonshot.cn/v1/chat/completions
        // 整个粘进 Base 框，先剥掉末尾的 /chat/completions，
        // 否则会跟 Retrofit 的 @POST("chat/completions") 拼成双路径 → 404。
        if (raw.endsWith("/chat/completions", ignoreCase = true)) {
            raw = raw.removeSuffix("/chat/completions").trimEnd('/')
        }
        // 容错 2：用户若漏填 /v1（最常见的填错），自动补上，
        // 避免请求落到 https://api.moonshot.cn/chat/completions 直接 404。
        val normalized = if (raw.endsWith("/v1", ignoreCase = true)) {
            raw
        } else {
            "$raw/v1"
        }
        if (normalized.endsWith("/")) normalized else "$normalized/"
    }

    private val api: OpenAiApi by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl(endpoint)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenAiApi::class.java)
    }

    override suspend fun analyzeBody(images: List<String>, profile: UserProfile?): BodyAnalysis {
        val raw = callVision(PromptBuilder.bodyPrompt(profile, images.size), images)
        return parseBody(raw)
    }

    override suspend fun analyzeFood(images: List<String>, profile: UserProfile?): FoodAnalysis {
        val raw = callVision(PromptBuilder.foodPrompt(profile, images.size), images)
        return parseFood(raw)
    }

    /**
     * 调用视觉接口。构建 1 个 text block + 对每张图各 1 个 image_url block。
     * @param textPrompt 提示词（单张/多张版）
     * @param images     压缩后的 base64 JPEG 列表（最多 4 张）
     */
    private suspend fun callVision(textPrompt: String, images: List<String>): String {
        val content = JsonArray().apply {
            add(JsonObject().apply {
                addProperty("type", "text")
                addProperty("text", textPrompt)
            })
            images.forEach { b64 ->
                add(JsonObject().apply {
                    addProperty("type", "image_url")
                    add("image_url", JsonObject().apply {
                        addProperty("url", "data:image/jpeg;base64,$b64")
                    })
                })
            }
        }
        val messages = JsonArray().apply {
            add(JsonObject().apply {
                addProperty("role", "system")
                addProperty("content", "你是严谨的健康分析助手，只输出符合要求的 JSON。")
            })
            add(JsonObject().apply {
                addProperty("role", "user")
                add("content", content)
            })
        }
        val body = JsonObject().apply {
            addProperty("model", model)
            add("messages", messages)
            add("response_format", JsonObject().apply { addProperty("type", "json_object") })
        }

        // Kimi k2.6 / k2.5 对 temperature / thinking 有严格限制：
        //   思考模式 temperature 固定 1.0，非思考模式固定 0.6，其它任何值都会直接 400。
        // 因此对这些模型统一「禁用思考 + temperature 0.6」：响应更快、更省 token，
        // 也避免默认开启的 thinking 带来不确定的推理输出。
        // 其它 OpenAI 兼容模型（如旧版 moonshot-v1-8k-vision-preview）保持原温度 0.4，且不传 thinking。
        if (model.contains("k2.6", ignoreCase = true) || model.contains("k2.5", ignoreCase = true)) {
            body.add("thinking", JsonObject().apply { addProperty("type", "disabled") })
            body.addProperty("temperature", 0.6)
        } else {
            body.addProperty("temperature", 0.4)
        }

        val resp = try {
            api.chat("Bearer $apiKey", body)
        } catch (e: HttpException) {
            // 非 2xx（如 401 无效 key、404 地址错、429 限流、400 参数错）会走到这里。
            // Retrofit 默认不会把错误体解析成 JsonObject，所以必须把 errorBody 读出来，
            // 否则真实原因会被外层 catch 吞掉，只看到“请检查网络与 API Key”。
            val errBody = runCatching { e.response()?.errorBody()?.string().orEmpty() }.getOrDefault("")
            val msg = runCatching {
                Gson().fromJson(errBody, JsonObject::class.java)
                    ?.optString("error")?.takeIf { it.isNotBlank() } ?: errBody
            }.getOrDefault(errBody)
            throw RuntimeException("视觉 API 请求失败(${e.code()})：${msg.take(300)}")
        }

        if (resp.has("error")) {
            val msg = resp.getAsJsonObject("error").optString("message")
            throw RuntimeException("视觉 API 返回错误：$msg")
        }

        val rawContent = resp.getAsJsonArray("choices")
            .get(0).asJsonObject
            .getAsJsonObject("message")
            .get("content").asString
        return sanitizeJson(rawContent)
    }

    /**
     * 容错：Kimi 等模型在 JSON Mode 下有时仍会用 ```json ... ``` 包裹输出，
     * Gson 直接解析会失败。这里把 markdown 代码块包裹剥掉，只保留纯 JSON 文本。
     */
    private fun sanitizeJson(raw: String): String {
        var s = raw.trim()
        if (s.startsWith("```")) {
            val firstNewline = s.indexOf('\n')
            if (firstNewline >= 0) s = s.substring(firstNewline + 1)
            if (s.endsWith("```")) s = s.removeSuffix("```")
            s = s.trim()
        }
        return s
    }

    private fun parseBody(json: String): BodyAnalysis {
        val root = Gson().fromJson(json, JsonObject::class.java)
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

    private fun parseFood(json: String): FoodAnalysis {
        val root = Gson().fromJson(json, JsonObject::class.java)
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

    private interface OpenAiApi {
        @POST("chat/completions")
        suspend fun chat(
            @Header("Authorization") auth: String,
            @Body body: JsonObject
        ): JsonObject
    }
}

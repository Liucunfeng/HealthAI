package com.example.healthai.data

import android.content.Context
import android.content.SharedPreferences

/**
 * 云端视觉 API 的配置 + 选人状态。
 *
 * 注意：这里用 SharedPreferences 明文保存 API Key，仅适合个人调试使用。
 * 若要做正式发布版本，建议改用 EncryptedSharedPreferences 或让用户登录后
 * 由后端代理转发请求，避免 Key 留在客户端。
 */
object AppPreferences {
    private const val NAME = "health_ai_prefs"
    private const val KEY_API_KEY = "api_key"
    private const val KEY_API_BASE = "api_base"
    private const val KEY_MODEL = "model"
    private const val KEY_ACTIVE_PROFILE = "active_profile_id"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun getApiKey(context: Context): String =
        prefs(context).getString(KEY_API_KEY, "") ?: ""

    fun setApiKey(context: Context, value: String) =
        prefs(context).edit().putString(KEY_API_KEY, value).apply()

    fun getApiBase(context: Context): String =
        prefs(context).getString(KEY_API_BASE, "") ?: ""

    fun setApiBase(context: Context, value: String) =
        prefs(context).edit().putString(KEY_API_BASE, value).apply()

    fun getModel(context: Context): String =
        prefs(context).getString(KEY_MODEL, "gpt-4o-mini") ?: "gpt-4o-mini"

    fun setModel(context: Context, value: String) =
        prefs(context).edit().putString(KEY_MODEL, value).apply()

    /**
     * 当前选中的身体档案 id。
     * 0 表示"未指定 / 通用建议"；非 0 且对应档案不存在时也视为通用建议。
     */
    fun getActiveProfileId(context: Context): Long =
        prefs(context).getLong(KEY_ACTIVE_PROFILE, 0L)

    fun setActiveProfileId(context: Context, id: Long) =
        prefs(context).edit().putLong(KEY_ACTIVE_PROFILE, id).apply()
}

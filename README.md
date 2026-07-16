# AI 健康助手（Android · Kotlin）

一个原生 Android（Kotlin）健康类 APP：拍照分析**身材比例与体质**、拍照识别**食物营养与热量**，
并结合你的个人身体档案，给出「这道食物适不适合你、不适合该怎么调整」的个性化建议。

> 核心的「图像理解」通过**云端视觉大模型 API** 完成（默认实现 OpenAI 兼容接口，
> 可一键切换到腾讯云/混元等）。其余数据（档案、历史）全部存在手机本地（Room），不上传。

---

## 功能一览

| 模块 | 说明 |
| --- | --- |
| 身材分析 | 拍照/相册选图 → 云端视觉 API 识别体型、比例指标、体质、体脂估算，并给出建议 |
| 食物分析 | 拍照/相册选图 → 识别食物种类、热量与三大营养素，结合档案判断适合度并给出调整建议 |
| 我的档案 | 身高 / 体重 / 年龄 / 性别 / 健康目标（减脂·维持·增肌）/ 活动水平，存本地 Room |
| 设置 | 填写云端视觉 API Key、API Base（可选）、模型名；一键清空历史 |
| 历史 | 所有分析记录（含缩略图、摘要、完整 JSON 明细）存本地，可回看 |

---

## 技术栈

- **语言**：Kotlin 1.9
- **UI**：View + ViewBinding + Material Design 3 + BottomNavigationView
- **本地存储**：Room（用户档案、分析历史）
- **网络**：Retrofit2 + OkHttp3 + Gson（调用视觉大模型）
- **并发**：Kotlin Coroutines
- **最小 SDK**：26（Android 8.0），目标/编译 SDK：34

---

## 环境要求

- Android Studio Hedgehog / Iguana 或更新版本
- Android SDK（compileSdk 34）、Android Build Tools
- 一台 Android 8.0+ 真机或模拟器（相机功能建议用真机）
- 一个可用的云端视觉 API Key（OpenAI 或任意 OpenAI 兼容服务）

---

## 导入与运行

1. 用 Android Studio 打开本目录（`HealthAIApp/`）作为项目。
2. 首次打开会自动下载 Gradle、AndroidX、Material、Room 等依赖（需要联网）。
3. 连接真机或启动模拟器，点击 ▶ Run。
4. 首次使用先到「设置」页填写 API Key，再到「我的」页填身体档案，之后即可拍照分析。

---

## 配置 API Key

进入 **设置** 页：

- **API Key**：粘贴你的 OpenAI Key（形如 `sk-...`）。
- **API Base URL（可选）**：留空使用默认 `https://api.openai.com/v1/`；
  若使用 Azure OpenAI、或自托管兼容网关，填对应地址（需以 `/` 结尾）。
- **模型名称**：默认 `gpt-4o-mini`；可换成 `gpt-4o`、`gpt-4-turbo` 等支持视觉的模型。

> 未配置 Key 时点「分析」会提示先去设置。

---

## 使用流程

1. 「我的」页填写身高/体重/年龄/性别/目标/活动量并保存（分析结果会据此个性化）。
2. 「身材」页拍照 → 点「分析身材」。
3. 「食物」页拍一张餐食 → 点「分析食物」，查看热量、营养素与适合度建议。
4. 「历史」页可回看每次分析。

---

## 项目结构

```
app/src/main/
├── AndroidManifest.xml
├── java/com/example/healthai/
│   ├── MainActivity.kt                 # 单 Activity + 底部导航
│   ├── data/
│   │   ├── AppDatabase.kt              # Room 数据库
│   │   ├── UserProfile.kt              # 用户档案（实体+DAO）
│   │   ├── AnalysisRecord.kt           # 分析历史（实体+DAO）
│   │   └── AppPreferences.kt           # API Key 等设置（SharedPreferences）
│   ├── vision/
│   │   ├── models.kt                   # 结构化结果模型
│   │   ├── VisionAnalyzer.kt           # 分析接口 + 工厂
│   │   ├── OpenAIVisionAnalyzer.kt     # OpenAI 兼容实现（默认）
│   │   └── TencentVisionAnalyzer.kt    # 腾讯云/混元适配模板
│   ├── util/
│   │   ├── ImageUtils.kt               # Uri→base64、base64→Bitmap
│   │   ├── JsonExt.kt                  # Gson 容错取值
│   │   └── PromptBuilder.kt            # 提示词构造
│   └── ui/
│       ├── BodyAnalysisFragment.kt     # 身材分析
│       ├── FoodAnalysisFragment.kt     # 食物分析
│       ├── ProfileFragment.kt          # 身体档案
│       ├── SettingsFragment.kt         # 设置
│       ├── HistoryFragment.kt          # 历史
│       └── HistoryAdapter.kt           # 历史列表适配器
└── res/                                 # 布局、字符串、主题、图标、菜单
```

---

## 如何接入真实模型 / 腾讯云

视觉分析层是**可插拔接口**（`VisionAnalyzer`），默认用 `OpenAIVisionAnalyzer`：

- **换 OpenAI 兼容服务**：在设置页改 API Base / 模型名即可，无需改代码。
- **接入腾讯云 / 混元**：
  打开 `vision/TencentVisionAnalyzer.kt`，顶部注释写了两条路线
  （混元多模态对话 / 腾讯云 AI 原子能力组合）。按注释补全实现后，
  在 `VisionAnalyzerFactory.create()` 里把 `OpenAIVisionAnalyzer(...)` 换成
  `TencentVisionAnalyzer()` 即可，上层 UI 完全不用动。

---

## 隐私与安全说明

- 用户档案与分析历史只存在本机 SQLite（Room），**不会上传**。
- 你拍的照片会以 base64 形式发给你所配置的视觉 API（这是「看图分析」所必需的）。
- API Key 当前以明文存在 `SharedPreferences`，**仅供个人调试**。
  若要正式发布，请改用 `EncryptedSharedPreferences`，或改为「App 调自己后端、后端持有 Key」的代理模式。

---

## 已知限制 / 后续可优化

- 视觉 API 返回的是模型「估算」值，仅供健康参考，不构成医疗建议。
- 历史详情弹窗直接展示原始 JSON，体验较糙，可改为结构化卡片。
- 可在 `build.gradle` 把 `minSdk` 降到 23（需额外处理 FileProvider/权限兼容）。
- 未接入端侧 ML（TensorFlow Lite）离线方案；如需完全离线识别可后续扩展。

# 云编译出 APK（GitHub Actions）

不用本地装 Android Studio。把工程推到 GitHub，GitHub 的服务器会自动帮你构建出 `app-debug.apk`，下载后传到手机即可安装。

## 流水线做了什么

`.github/workflows/build-apk.yml` 会在每次 push 到 `main`/`master` 或你手动触发时：

1. 拉取代码
2. 安装 JDK 17 + Android SDK（platform 34、build-tools 34.0.0）
3. 用 Gradle 8.9 执行 `assembleDebug`（不依赖本地的 gradlew）
4. 把生成的 APK 上传为 Actions Artifact

构建环境对齐工程配置：AGP 8.2.2、Kotlin 1.9.22、compileSdk/targetSdk 34、minSdk 26、JDK 17。

## 使用步骤

### 1. 推到 GitHub（仓库已帮你 git init + 初次提交）

在你的 GitHub 新建一个**空仓库**（不要勾选 README/.gitignore），然后：

```bash
cd HealthAIApp
git remote add origin https://github.com/<你的用户名>/<仓库名>.git
git branch -M main
git push -u origin main
```

> 如果你还没配置 git 身份，先执行：
> `git config --global user.email "you@example.com"` 和 `git config --global user.name "你的名字"`

### 2. 触发构建

- 推送后构建会自动开始；或进入仓库 **Actions → Build Debug APK → Run workflow** 手动触发。
- 首次构建约 5–10 分钟（要下载 Gradle、Android SDK 和各依赖库）。

### 3. 下载 APK

构建完成后：

1. 进入仓库 **Actions → 对应那次构建 → 底部 Artifacts 区**
2. 下载 `app-debug` 压缩包，解压得到 `app-debug.apk`
3. 通过微信 / 数据线 / 网盘传到手机，点开按提示允许「未知来源」安装

### 4. 填 Key 使用

装好后，进 APP「设置」填云端视觉 API Key（及 Base/Model），再填「我的」档案，即可拍照做身材 / 食物分析。

## 构建失败排查

- **红叉在 "Install Android SDK packages"**：多为 SDK 许可未接受，工作流已加 `yes | sdkmanager --licenses`；若仍失败检查 `android-actions/setup-android` 版本。
- **红叉在 "Build with Gradle"**：展开日志看具体错误。常见是依赖下载超时（重试即可）或 Kotlin/AGP 版本不匹配（本工程已对齐，勿随意升级）。
- **找不到 APK（Upload 步骤报错）**：说明 `assembleDebug` 没产出，多半是编译错误，回头看 Gradle 日志。

## 进阶

- **自动发 release 包 / 自动发到手机**：可加 `softprops/action-gh-release` 把 APK 作为 Release 资产。
- **正式发布签名**：debug 包用的是自动 debug 签名，不能直接上架。要上架请改用 `Build → Generate Signed Bundle/APK` 或在工作流里配置 signing 密钥（建议把 keystore 放 GitHub Secrets）。
- **想用国内 CI**（避免 GitHub 访问慢）：可把同一套步骤搬到 Gitee CI、或自建 Runner。

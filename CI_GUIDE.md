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

## git push 认证失败（Password authentication is not supported）

GitHub 自 2021-08 起**禁止用登录密码做 Git 操作**。若在 `Password for...` 处填了网页登录密码会报 `Authentication failed / Password authentication is not supported`。改用以下任一方式：

**方式一：HTTPS + 个人访问令牌（PAT，最省事）**
1. GitHub → 头像 → Settings → Developer settings → Personal access tokens → Tokens (classic) → Generate new token (classic)
2. Note 随意填；Expiration 选期限；勾选 **repo**（整组）以获仓库读写权
3. 最底部 Generate token，**复制 token（只显示这一次）**
4. 重新 push：`Username` 填 GitHub 用户名，`Password` **粘贴 token（不是登录密码）**
5. macOS 记住凭证（免重复输入）：`git config --global credential.helper osxkeychain`

**方式二：SSH 密钥（更安全、一次配置）**
1. 生成：`ssh-keygen -t ed25519 -C "你的邮箱"`（一路回车）
2. 复制公钥：`cat ~/.ssh/id_ed25519.pub`
3. GitHub → Settings → SSH and GPG keys → New SSH key 粘贴保存
4. 改 remote 为 SSH 并推送：
   `git remote set-url origin git@github.com:Liucunfeng/HealthAI.git`
   `git push -u origin main`

> token 等同密码，勿泄露；泄露后到 GitHub 撤销重发。临时一次性用法也可 `git remote set-url origin https://<TOKEN>@github.com/Liucunfeng/HealthAI.git`（token 会明文存于 git config，不建议长期使用）。

## 构建时的 "Node.js 20 is deprecated" 警告（非错误）

构建日志顶部若出现类似下面一段**黄色**提示：

> Node.js 20 is deprecated. The following actions target Node.js 20 but are being forced to run on Node.js 24: actions/checkout@v4, actions/setup-java@v4, ...

**这是弃用警告，不是构建错误**，不会导致失败。GitHub 自 2025-09 起把 Runner 上的 Node 20 强制换成 Node 24，仍 target Node 20 的 action 会被自动兼容运行，只是打印这条提醒。

- 只要 workflow run 的状态是绿色 ✅、且底部能下载 `app-debug` 这个 Artifact，**直接忽略它**，解压装到手机即可。
- 若想日志干净，本仓库工作流已做处理：移除第三方 `android-actions/setup-android`（改用官方 `cmdline-tools` 手动装 SDK）、`gradle-build-action` 升到 `v4`。剩下的 `checkout@v4` / `setup-java@v4` 是 GitHub 官方 action，其 Node 20 提醒无害，待官方发补丁即可消除，无需你处理。
- **真正的失败**表现为某个 step 红色 ❌，且日志里有具体报错（如 `sdkmanager` 失败、`assembleDebug` 编译错误）。那种才需要排查——把红色部分发我即可。

## 进阶

- **自动发 release 包 / 自动发到手机**：可加 `softprops/action-gh-release` 把 APK 作为 Release 资产。
- **正式发布签名**：debug 包用的是自动 debug 签名，不能直接上架。要上架请改用 `Build → Generate Signed Bundle/APK` 或在工作流里配置 signing 密钥（建议把 keystore 放 GitHub Secrets）。
- **想用国内 CI**（避免 GitHub 访问慢）：可把同一套步骤搬到 Gitee CI、或自建 Runner。

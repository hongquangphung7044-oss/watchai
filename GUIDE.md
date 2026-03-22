# WatchAI - 一加手表2 LLM 聊天客户端
## 完整安装指南（适合零基础）

---

## 📦 项目文件说明

```
watchai/
├── gradle/
│   └── libs.versions.toml          ← 依赖版本配置
├── app/
│   ├── build.gradle.kts            ← app 模块构建配置
│   └── src/main/
│       ├── AndroidManifest.xml     ← 权限声明
│       ├── res/values/
│       │   ├── strings.xml         ← 字符串资源
│       │   └── themes.xml          ← 主题（纯黑）
│       └── java/com/example/watchai/
│           ├── MainActivity.kt                    ← 入口 Activity
│           ├── data/
│           │   ├── PreferencesRepository.kt       ← 保存 API 配置
│           │   └── ChatApi.kt                     ← OpenAI 兼容流式 API
│           ├── viewmodel/
│           │   └── ChatViewModel.kt               ← 核心业务逻辑
│           └── ui/
│               ├── WatchAiApp.kt                  ← 导航入口
│               ├── theme/Theme.kt                 ← 深色主题
│               └── screens/
│                   ├── ChatScreen.kt              ← 聊天主界面
│                   └── SetupScreen.kt             ← 设置界面
├── build.gradle.kts                ← 根构建配置
└── settings.gradle.kts             ← 项目设置
```

---

## 🛠️ 第一步：安装 Android Studio

1. 打开 https://developer.android.com/studio
2. 下载并安装 Android Studio（推荐 Ladybug 2024.2 或更新版本）
3. 安装时全部默认选项即可

---

## 📁 第二步：创建新项目

1. 打开 Android Studio
2. 点击 **「New Project」**
3. 选择 **「Empty Activity」**（不是 Wear OS 的！）
4. 填写以下信息：
   - **Name**：`WatchAI`
   - **Package name**：`com.example.watchai`
   - **Save location**：任意你想要的位置
   - **Language**：`Kotlin`
   - **Minimum SDK**：`API 30 (Android 11)`
5. 点击 **「Finish」**，等待项目创建完成

---

## 📋 第三步：替换文件

将本目录中的文件，**逐一复制替换**到对应位置：

### 替换根目录文件
| 本指南文件 | 替换项目中的文件 |
|---|---|
| `build.gradle.kts` | 项目根目录的 `build.gradle.kts` |
| `settings.gradle.kts` | 项目根目录的 `settings.gradle.kts` |
| `gradle/libs.versions.toml` | `gradle/libs.versions.toml` |

### 替换 app 模块文件
| 本指南文件 | 替换项目中的文件 |
|---|---|
| `app/build.gradle.kts` | `app/build.gradle.kts` |
| `app/src/main/AndroidManifest.xml` | `app/src/main/AndroidManifest.xml` |
| `app/src/main/res/values/strings.xml` | `app/src/main/res/values/strings.xml` |

### 新建文件（直接复制进去）
将 `app/src/main/res/values/themes.xml` 复制到项目的 `app/src/main/res/values/` 目录

### 替换 Kotlin 源码
在项目的 `app/src/main/java/com/example/watchai/` 目录下：

1. **删除**原有的 `MainActivity.kt`，**复制**本项目的 `MainActivity.kt`
2. **新建**文件夹 `data`，复制进去：`PreferencesRepository.kt`、`ChatApi.kt`
3. **新建**文件夹 `viewmodel`，复制进去：`ChatViewModel.kt`
4. **新建**文件夹 `ui`，在里面：
   - 复制 `WatchAiApp.kt`
   - **新建**子文件夹 `theme`，复制进去 `Theme.kt`
   - **新建**子文件夹 `screens`，复制进去 `ChatScreen.kt`、`SetupScreen.kt`

---

## 🔨 第四步：同步并编译

1. 替换完文件后，点击 Android Studio 顶部黄色提示条的 **「Sync Now」**
   （或菜单 File → Sync Project with Gradle Files）
2. 等待同步完成（第一次可能需要下载依赖，耗时 3-10 分钟，需要网络）
3. 同步成功后，点击顶部菜单 **Build → Make Project**
4. 底部 Build 窗口出现 `BUILD SUCCESSFUL` 即成功

**常见报错解决：**
- `Unresolved reference` → 检查文件是否放对位置
- `Gradle sync failed` → 检查网络，或换镜像源（见下方）
- `minSdk` 错误 → 确认 app/build.gradle.kts 中 minSdk = 30

---

## 📱 第五步：安装到手表

### 方法一：通过电脑 ADB 安装（推荐）

1. 手表开启**开发者模式**：
   - 进入手表设置 → 关于 → 版本号
   - 连续点击版本号 7 次
   - 返回设置，找到「开发者选项」
   - 开启「ADB 调试」和「通过 Wi-Fi 调试」

2. 手表和电脑连接到**同一 Wi-Fi**

3. 在手表开发者选项中，查看 ADB over Wi-Fi 的 **IP地址:端口**（例如 `192.168.1.5:5555`）

4. 在电脑终端（Windows 用 PowerShell，Mac 用 Terminal）执行：
   ```bash
   # 连接手表
   adb connect 192.168.1.5:5555
   
   # 确认连接成功（应显示 connected）
   adb devices
   ```

5. 在 Android Studio 顶部设备栏，选择你的手表，点击 ▶ 运行

### 方法二：手动传输 APK

1. Android Studio 菜单 → Build → Build Bundle(s)/APK(s) → **Build APK(s)**
2. 等待编译，点击提示中的「locate」找到 APK 文件（在 `app/build/outputs/apk/debug/`）
3. 将 APK 传到手表（通过手机蓝牙、文件传输等方式）
4. 在手表上打开文件管理器，点击 APK 安装

---

## ⚙️ 第六步：配置 API

安装完成后，首次打开 App：

1. 点击右上角 **齿轮图标（设置）**
2. 填写 **API 地址** 和 **API Key**

### 推荐配置（适合国内用户）

**方案1 - DeepSeek（便宜好用）：**
- API 地址：`https://api.deepseek.com/v1`
- API Key：去 https://platform.deepseek.com 注册获取
- 模型：`deepseek-chat`

**方案2 - 月之暗面 Moonshot（中文对话好）：**
- API 地址：`https://api.moonshot.cn/v1`
- API Key：去 https://platform.moonshot.cn 注册获取
- 模型：`moonshot-v1-8k`

**方案3 - 阿里通义千问：**
- API 地址：`https://dashscope.aliyuncs.com/compatible-mode/v1`
- API Key：去 https://dashscope.aliyun.com 注册获取
- 模型：`qwen-plus`

**方案4 - OpenAI（需要能访问）：**
- API 地址：`https://api.openai.com/v1`
- API Key：`sk-…`
- 模型：`gpt-4o-mini`

3. 点击 **「保存并返回」**

---

## 🎙️ 使用说明

### 基本操作
- **蓝色麦克风按钮** → 按下后对着手表说话 → 自动发送给 AI
- **右上角设置图标** → 修改 API 配置
- **右上角垃圾桶** → 清除当前对话，开始新对话
- **喇叭关闭图标** → 停止 AI 语音朗读（TTS）

### 语音输入说明
- App 使用手表系统内置的语音识别（OPPO/一加系统自带）
- 不需要 Google 服务
- 首次使用可能需要授权麦克风权限，点击「允许」即可
- 支持普通话，部分方言也支持

### AI 回复
- AI 的文字回复会**流式显示**（边生成边出现，和 ChatGPT 一样）
- 回复完成后**自动朗读**（TTS 语音播报），方便不看屏幕时听
- 每次对话最多 600 字回复（手表屏幕限制，可在代码中调整）

---

## 🔧 进阶调整

### 修改最大回复长度
打开 `ChatApi.kt`，找到这一行：
```kotlin
put("max_tokens", 600)
```
改成你想要的数字，例如 `1000`

### 修改默认系统提示词
打开 `PreferencesRepository.kt`，找到：
```kotlin
systemPrompt = prefs[KEY_SYSTEM_PROMPT] ?: "你是手表上的AI助手，回答要简洁，因为屏幕很小。"
```
修改引号中的内容

### 添加更多快捷服务商
打开 `SetupScreen.kt`，找到这个列表：
```kotlin
listOf(
    "OpenAI"   to "https://api.openai.com/v1",
    "DeepSeek" to "https://api.deepseek.com/v1",
    ...
)
```
按格式添加新的服务商

---

## ❓ 常见问题

**Q: 语音识别不工作？**
A: 确认手表麦克风权限已开启。在手表设置 → 应用 → WatchAI → 权限 → 麦克风 → 允许

**Q: API 报错 401？**
A: API Key 填写错误，请检查

**Q: API 报错 404？**
A: API 地址可能缺少 `/v1`，注意末尾格式

**Q: 回复很慢？**
A: 正常，取决于网络和 API 服务商速度。DeepSeek 国内速度较快

**Q: 朗读没有声音？**
A: 检查手表音量。也可能是系统 TTS 未安装中文语音包

**Q: 安装时提示「未知来源」？**
A: 手表设置 → 安全 → 允许安装未知来源应用

---

## 📊 技术架构说明

本 App 完全参考 rikkahub 的 API 协议，使用相同的 **OpenAI 兼容接口**：

```
用户语音 → 系统语音识别(RecognizerIntent)
    ↓
文字 → ChatViewModel → ChatApi
    ↓
OkHttp POST /chat/completions (SSE 流式)
    ↓
实时接收 delta → 更新 UI
    ↓
完成 → TextToSpeech 朗读
```

支持所有 rikkahub 支持的 API 服务商（OpenAI / DeepSeek / Moonshot / 通义 / Ollama 等）

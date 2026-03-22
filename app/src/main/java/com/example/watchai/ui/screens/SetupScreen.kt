package com.example.watchai.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.watchai.data.AppSettings
import com.example.watchai.data.ProviderConfig
import com.example.watchai.viewmodel.ChatViewModel
import com.example.watchai.viewmodel.Screen

@Composable
fun SetupScreen(viewModel: ChatViewModel) {
    val context         = LocalContext.current
    val settings        by viewModel.settings.collectAsState()
    val uiState         by viewModel.uiState.collectAsState()
    val availableModels by viewModel.availableModels.collectAsState()
    val providers       by viewModel.providers.collectAsState()
    val currentFontSize by viewModel.fontSize.collectAsState()

    var baseUrl      by remember(settings.baseUrl)      { mutableStateOf(settings.baseUrl) }
    var apiKey       by remember(settings.apiKey)       { mutableStateOf(settings.apiKey) }
    var model        by remember(settings.model)        { mutableStateOf(settings.model) }
    var systemPrompt by remember(settings.systemPrompt) { mutableStateOf(settings.systemPrompt) }
    var showApiKey   by remember { mutableStateOf(false) }
    var saved        by remember { mutableStateOf(false) }
    var showSaveDialog  by remember { mutableStateOf(false) }
    var configName      by remember { mutableStateOf("") }
    var msg             by remember { mutableStateOf("") }

    // 配置TXT导入
    val configTxtLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val content = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()?.readText() ?: return@rememberLauncherForActivityResult
            val result = viewModel.importFromTxt(content)
            if (result != null) {
                baseUrl = result.first; apiKey = result.second; model = result.third
                saved = false; msg = "✅ 配置导入成功"
            } else { msg = "❌ 格式错误：第1行地址，第2行密钥" }
        } catch (_: Exception) { msg = "❌ 读取失败" }
    }

    // 提示词TXT导入
    val promptTxtLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val content = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()?.readText() ?: return@rememberLauncherForActivityResult
            systemPrompt = viewModel.importPromptFromTxt(content)
            saved = false; msg = "✅ 提示词导入成功"
        } catch (_: Exception) { msg = "❌ 读取失败" }
    }

    // 保存配置弹窗
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = Color(0xFF162130),
            title = { Text("保存配置", color = Color.White, fontSize = 14.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("配置名称（如 DeepSeek工作）", color = Color(0xFF90A4AE), fontSize = 11.sp)
                    OutlinedTextField(
                        value = configName, onValueChange = { configName = it },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = Color(0xFF2B6CB0),
                            unfocusedBorderColor = Color(0xFF1E3A5F),
                            cursorColor          = Color(0xFF63B3ED)
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (configName.isNotBlank()) {
                        viewModel.saveProviderConfig(configName.trim(), baseUrl, apiKey, model)
                        showSaveDialog = false; configName = ""; msg = "✅ 配置已保存"
                    }
                }) { Text("保存", color = Color(0xFF63B3ED), fontSize = 13.sp) }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false; configName = "" }) {
                    Text("取消", color = Color(0xFF718096), fontSize = 13.sp)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1520))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 22.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 顶部栏
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(Screen.CHAT) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Text("设置", color = Color.White, fontSize = 14.sp)
            IconButton(
                onClick = {
                    viewModel.saveSettings(AppSettings(baseUrl, apiKey, model, systemPrompt))
                    saved = true
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Check, null,
                    tint = if (saved) Color(0xFF48BB78) else Color(0xFF63B3ED),
                    modifier = Modifier.size(18.dp))
            }
        }

        // 字体大小
        SectionLabel("字体大小")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape)
                    .background(Color(0xFF1A2535))
                    .border(1.dp, Color(0xFF2D4A6A), CircleShape)
                    .clickable(enabled = currentFontSize > 11) { viewModel.setFontSize(currentFontSize - 1) },
                contentAlignment = Alignment.Center
            ) { Text("A", color = if (currentFontSize > 11) Color(0xFF90A4AE) else Color(0xFF2D3748), fontSize = 10.sp) }

            Text("${currentFontSize} sp", color = Color(0xFFCBD5E0), fontSize = 13.sp)

            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape)
                    .background(Color(0xFF1A2535))
                    .border(1.dp, Color(0xFF2D4A6A), CircleShape)
                    .clickable(enabled = currentFontSize < 18) { viewModel.setFontSize(currentFontSize + 1) },
                contentAlignment = Alignment.Center
            ) { Text("A", color = if (currentFontSize < 18) Color(0xFFE2E8F0) else Color(0xFF2D3748), fontSize = 16.sp) }
        }

        // 已保存配置
        if (providers.isNotEmpty()) {
            SectionLabel("已保存配置")
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF141F2E)).padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                providers.forEach { cfg ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                            .background(if (baseUrl == cfg.baseUrl && apiKey == cfg.apiKey) Color(0xFF1A3A5C) else Color.Transparent)
                            .clickable { baseUrl = cfg.baseUrl; apiKey = cfg.apiKey; model = cfg.model; saved = false }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(cfg.name, color = Color(0xFFE2E8F0), fontSize = 12.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(cfg.model, color = Color(0xFF718096), fontSize = 10.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { viewModel.deleteProviderConfig(cfg.id) }, modifier = Modifier.size(22.dp)) {
                            Icon(Icons.Default.Close, null, tint = Color(0xFF4A5568), modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }
        }

        // 快捷服务商
        SectionLabel("服务商")
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf("OpenAI" to "https://api.openai.com/v1",
                   "DeepSeek" to "https://api.deepseek.com/v1",
                   "月之暗面" to "https://api.moonshot.cn/v1"
            ).forEach { (n, u) -> QuickChip(n, baseUrl == u) { baseUrl = u; saved = false } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf("通义千问" to "https://dashscope.aliyuncs.com/compatible-mode/v1",
                   "Ollama" to "http://192.168.1.100:11434/v1"
            ).forEach { (n, u) -> QuickChip(n, baseUrl == u) { baseUrl = u; saved = false } }
        }

        // API 地址
        SettingField("API 地址", baseUrl, "https://api.openai.com/v1",
            onChange = { baseUrl = it; saved = false })

        // API Key
        SettingField("API Key", apiKey, "sk-…",
            onChange = { apiKey = it; saved = false },
            isPassword = !showApiKey,
            trailingIcon = {
                IconButton(onClick = { showApiKey = !showApiKey }, modifier = Modifier.size(24.dp)) {
                    Icon(if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        null, tint = Color(0xFF718096), modifier = Modifier.size(15.dp))
                }
            }
        )

        // 导入配置TXT + 保存配置
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionBtn("导入配置TXT", Icons.Default.FileOpen, Modifier.weight(1f)) {
                configTxtLauncher.launch("text/plain")
            }
            ActionBtn("保存配置", Icons.Default.BookmarkAdd, Modifier.weight(1f)) {
                showSaveDialog = true
            }
        }

        // 模型
        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("模型")
            ActionBtn(
                label = if (uiState.isFetchingModels) "获取中…" else "搜索可用模型",
                icon  = if (uiState.isFetchingModels) Icons.Default.Sync else Icons.Default.Search,
                enabled = !uiState.isFetchingModels
            ) { viewModel.fetchModels(baseUrl, apiKey) }
        }

        SettingField("", model, "gpt-4o-mini", onChange = { model = it; saved = false })

        AnimatedVisibility(visible = availableModels.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("可用模型 (${availableModels.size})", color = Color(0xFF718096), fontSize = 11.sp)
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF141F2E)).padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    availableModels.forEach { m ->
                        ModelRadioRow(m, model == m) { model = m; saved = false }
                    }
                }
            }
        }

        if (uiState.error != null) {
            Text(uiState.error ?: "", color = Color(0xFFFC8181), fontSize = 11.sp)
        }

        // 系统提示词
        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("系统提示词")
            ActionBtn("导入TXT", Icons.Default.FileOpen) {
                promptTxtLauncher.launch("text/plain")
            }
        }
        SettingField("", systemPrompt, "你是手表上的AI助手…",
            onChange = { systemPrompt = it; saved = false },
            singleLine = false, maxLines = 5)

        // 提示/状态信息
        AnimatedVisibility(visible = msg.isNotBlank(), enter = fadeIn(), exit = fadeOut()) {
            Text(msg, color = if (msg.startsWith("✅")) Color(0xFF48BB78) else Color(0xFFFC8181),
                fontSize = 11.sp)
        }

        if (saved) {
            Text("✅ 已保存", color = Color(0xFF48BB78), fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        Spacer(Modifier.height(4.dp))

        // 保存并返回
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF2B6CB0))
                .clickable {
                    viewModel.saveSettings(AppSettings(baseUrl, apiKey, model, systemPrompt))
                    saved = true
                    viewModel.navigateTo(Screen.CHAT)
                }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) { Text("保存并返回", color = Color.White, fontSize = 13.sp) }

        Text("配置TXT：第1行地址，第2行密钥，第3行模型(可选)\n提示词TXT：整个文件内容作为提示词",
            color = Color(0xFF2D3748), fontSize = 9.sp, lineHeight = 13.sp)
    }
}

@Composable private fun SectionLabel(text: String) {
    Text(text, color = Color(0xFF718096), fontSize = 11.sp)
}

@Composable private fun ActionBtn(
    label: String, icon: ImageVector,
    modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit
) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF141F2E))
            .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 7.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (enabled) Color(0xFF63B3ED) else Color(0xFF2D4A6A), modifier = Modifier.size(12.dp))
            Text(label, color = if (enabled) Color(0xFF63B3ED) else Color(0xFF2D4A6A), fontSize = 10.sp)
        }
    }
}

@Composable private fun ModelRadioRow(name: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
            .background(if (selected) Color(0xFF1A3A5C) else Color.Transparent)
            .clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(13.dp).clip(CircleShape).background(Color.Transparent)
                .border(1.5.dp, if (selected) Color(0xFF63B3ED) else Color(0xFF2D4A6A), CircleShape),
            contentAlignment = Alignment.Center
        ) { if (selected) Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF63B3ED))) }
        Text(name, color = if (selected) Color(0xFFBEE3F8) else Color(0xFF718096),
            fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable private fun QuickChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFF1A3A5C) else Color(0xFF141F2E))
            .border(if (selected) 1.dp else 0.dp,
                if (selected) Color(0xFF63B3ED) else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 5.dp)
    ) { Text(label, color = if (selected) Color(0xFFBEE3F8) else Color(0xFF718096), fontSize = 10.sp) }
}

@Composable private fun SettingField(
    label: String, value: String, placeholder: String = "",
    onChange: (String) -> Unit, isPassword: Boolean = false,
    singleLine: Boolean = true, maxLines: Int = 1,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        if (label.isNotBlank()) Text(label, color = Color(0xFF718096), fontSize = 11.sp)
        OutlinedTextField(
            value = value, onValueChange = onChange,
            placeholder = { Text(placeholder, fontSize = 11.sp, color = Color(0xFF2D4A6A)) },
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            singleLine = singleLine, maxLines = maxLines, trailingIcon = trailingIcon,
            modifier = Modifier.fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = Color.White),
            keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF2B6CB0), unfocusedBorderColor = Color(0xFF1E2D3D),
                cursorColor = Color(0xFF63B3ED)
            )
        )
    }
}

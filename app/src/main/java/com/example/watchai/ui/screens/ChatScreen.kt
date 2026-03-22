package com.example.watchai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.watchai.data.ChatMessage
import com.example.watchai.ui.MarkdownText
import com.example.watchai.viewmodel.ChatViewModel
import com.example.watchai.viewmodel.Screen

// ── 圆形屏 466×466px ≈ 228dp 直径，半径 114dp ──────────────
// 距顶/底 28dp 处：安全内边距 ≥ 39dp → 用 40dp
// 距顶/底 38dp 处：安全内边距 ≥ 29dp → 用 30dp（消息区域）
// 底部圆按钮距底 28dp，水平居中，在安全区内 ✓

private val SAFE_PAD_TOP   = 40.dp   // 顶部图标区左右安全边距
private val SAFE_PAD_BOT   = 30.dp   // 消息区左右边距（中间位置更宽）

private val THINKING_OPTIONS = listOf(
    "none" to "思考:关", "low" to "思考:低",
    "medium" to "思考:中", "high" to "思考:高"
)

@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val messages        by viewModel.messages.collectAsState()
    val uiState         by viewModel.uiState.collectAsState()
    val settings        by viewModel.settings.collectAsState()
    val fontSizePx      by viewModel.fontSize.collectAsState()
    val reasoningEffort by viewModel.reasoningEffort.collectAsState()
    val listState       = rememberLazyListState()

    var showInputDialog by remember { mutableStateOf(false) }
    var inputText       by remember { mutableStateOf("") }
    val focusRequester  = remember { FocusRequester() }

    LaunchedEffect(messages.size, messages.lastOrNull()?.content?.length) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    // ── 输入弹窗 ───────────────────────────────────────────────
    if (showInputDialog) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(80)
            try { focusRequester.requestFocus() } catch (_: Exception) {}
        }
        Dialog(onDismissRequest = { showInputDialog = false; inputText = "" }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF162130))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    placeholder = {
                        Text("说点什么… 键盘含🎤", color = Color(0xFF2D4A6A), fontSize = 12.sp)
                    },
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        val t = inputText.trim()
                        if (t.isNotEmpty() && !uiState.isLoading) {
                            viewModel.sendMessage(t); inputText = ""; showInputDialog = false
                        }
                    }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor     = Color.White,
                        unfocusedTextColor   = Color.White,
                        focusedBorderColor   = Color(0xFF2B6CB0),
                        unfocusedBorderColor = Color(0xFF1E3A5F),
                        cursorColor          = Color(0xFF63B3ED)
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = fontSizePx.sp, color = Color.White)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (inputText.isNotBlank() && !uiState.isLoading)
                                Color(0xFF2B6CB0) else Color(0xFF1A2535)
                        )
                        .clickable(enabled = inputText.isNotBlank() && !uiState.isLoading) {
                            val t = inputText.trim()
                            if (t.isNotEmpty()) {
                                viewModel.sendMessage(t); inputText = ""; showInputDialog = false
                            }
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("发送", color = Color.White, fontSize = 13.sp)
                }
            }
        }
    }

    // ── 主界面：纯 Box，所有元素悬浮 ─────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1520))
    ) {
        // ── 消息区域（铺满全屏，用 contentPadding 避开悬浮元素）
        // 关键：modifier 不加 top/bottom padding，背景铺满
        // contentPadding 让第一条和最后一条消息不被遮挡
        if (messages.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("💬", fontSize = 22.sp)
                    Text("点下方按钮开始对话",
                        color = Color(0xFF2D3748), fontSize = 13.sp,
                        textAlign = TextAlign.Center)
                    if (settings.apiKey.isBlank()) {
                        Text("⚙️ 请先填写 API Key",
                            color = Color(0xFFE53E3E), fontSize = 11.sp,
                            modifier = Modifier.clickable { viewModel.navigateTo(Screen.SETUP) })
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                // 只加水平 padding，不加上下 padding，背景铺满屏幕
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = SAFE_PAD_BOT),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                // contentPadding 确保第一/最后一条不被悬浮元素遮住
                contentPadding = PaddingValues(top = 54.dp, bottom = 84.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    MessageBubble(msg, fontSizePx)
                }
            }
        }

        // ── 左上：思考强度（悬浮，距顶28dp，距左40dp = 安全区）
        val thinkingLabel = THINKING_OPTIONS.find { it.first == reasoningEffort }?.second ?: "思考:关"
        // 判断当前模型是否支持思考（与 ViewModel 保持一致）
        val modelName = settings.model.lowercase()
        val isThinkingSupported = modelName.contains("r1") || modelName.contains("o1") ||
            modelName.contains("o3") || modelName.contains("qwq") ||
            modelName.contains("thinking") || modelName.contains("reasoner")
        val thinkingActive = reasoningEffort != "none" && isThinkingSupported
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 28.dp, start = SAFE_PAD_TOP)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (thinkingActive) Color(0x881A3A5C) else Color(0x88111D2A)
                )
                .clickable {
                    val idx = THINKING_OPTIONS.indexOfFirst { it.first == reasoningEffort }
                    val next = THINKING_OPTIONS[(idx + 1) % THINKING_OPTIONS.size].first
                    viewModel.setReasoningEffort(next)
                }
                .padding(horizontal = 7.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (!isThinkingSupported && reasoningEffort != "none")
                    "$thinkingLabel·无效" else thinkingLabel,
                color = when {
                    thinkingActive            -> Color(0xFF63B3ED)
                    reasoningEffort != "none" -> Color(0xFF718096)
                    else                      -> Color(0xFF4A5568)
                },
                fontSize = 9.sp
            )
        }

        // ── 右上：图标（悬浮，距顶24dp，距右40dp = 安全区）
        // 3个图标×26dp = 78dp，在150dp可用宽度内完全安全
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 24.dp, end = SAFE_PAD_TOP),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(Screen.HISTORY) },
                modifier = Modifier.size(26.dp)
            ) {
                Icon(Icons.Default.History, null,
                    tint = Color(0xFF4A6080), modifier = Modifier.size(14.dp))
            }
            if (messages.isNotEmpty()) {
                IconButton(
                    onClick = { viewModel.newConversation() },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(Icons.Default.AddComment, null,
                        tint = Color(0xFF4A6080), modifier = Modifier.size(14.dp))
                }
            }
            IconButton(
                onClick = { viewModel.navigateTo(Screen.SETUP) },
                modifier = Modifier.size(26.dp)
            ) {
                Icon(Icons.Default.Settings, null,
                    tint = Color(0xFF4A6080), modifier = Modifier.size(14.dp))
            }
        }

        // ── 错误提示（悬浮，输入按钮上方）────────────────────
        AnimatedVisibility(
            visible = uiState.error != null,
            enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 76.dp)
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xCC0D1520))
                    .clickable { viewModel.clearError() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(uiState.error ?: "",
                    color = Color(0xFFFC8181), fontSize = 9.sp,
                    textAlign = TextAlign.Center)
            }
        }

        // ── 加载指示（悬浮）──────────────────────────────────
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 76.dp)
                    .size(12.dp),
                strokeWidth = 1.5.dp,
                color = Color(0xFF63B3ED)
            )
        }

        // ── 底部圆形输入按钮（悬浮，屏幕正下方，距底28dp）───
        // 按钮居中 = 水平圆心，距底28dp = 完全在圆形安全区
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp)
                .size(46.dp)
                .clip(CircleShape)
                .background(
                    if (uiState.isLoading) Color(0xFF1A2535) else Color(0xFF2B6CB0)
                )
                .clickable(enabled = !uiState.isLoading) { showInputDialog = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Edit, null,
                tint = if (uiState.isLoading) Color(0xFF2D4A6A) else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ── 消息气泡 ──────────────────────────────────────────────────
@Composable
private fun MessageBubble(message: ChatMessage, fontSizePx: Int) {
    val isUser = message.role == "user"
    val fs     = fontSizePx.sp

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(
                    topStart    = 10.dp, topEnd = 10.dp,
                    bottomStart = if (isUser) 10.dp else 2.dp,
                    bottomEnd   = if (isUser) 2.dp else 10.dp
                ))
                .background(if (isUser) Color(0xFF1A3A5C) else Color(0xFF1C2A3A))
                .padding(horizontal = 9.dp, vertical = 6.dp)
                .widthIn(max = 175.dp)
        ) {
            when {
                message.content.isEmpty() && !isUser -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 5.dp)
                    ) {
                        repeat(3) {
                            Box(Modifier.size(4.dp).clip(CircleShape).background(Color(0xFF4A5568)))
                        }
                    }
                }
                isUser -> Text(
                    text = message.content,
                    color = Color(0xFFBEE3F8),
                    fontSize = fs,
                    lineHeight = (fontSizePx * 1.45f).sp
                )
                else -> MarkdownText(
                    text = message.content,
                    fontSize = fs,
                    color = Color(0xFFE2E8F0)
                )
            }
        }
    }
}

package com.example.watchai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.watchai.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UiState(
    val isLoading: Boolean        = false,
    val isFetchingModels: Boolean = false,
    val error: String?            = null,
    val screen: Screen            = Screen.CHAT
)

enum class Screen { CHAT, SETUP, HISTORY }

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = PreferencesRepository(application)
    private val chatApi   = ChatApi()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _availableModels = MutableStateFlow<List<String>>(emptyList())
    val availableModels: StateFlow<List<String>> = _availableModels.asStateFlow()

    val settings: StateFlow<AppSettings> = prefsRepo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val history: StateFlow<List<ConversationSnapshot>> = prefsRepo.historyFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val providers: StateFlow<List<ProviderConfig>> = prefsRepo.providersFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val fontSize: StateFlow<Int> = prefsRepo.fontSizeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 13)

    val reasoningEffort: StateFlow<String> = prefsRepo.reasoningEffortFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "none")

    /**
     * 当前对话的稳定 ID。
     * 第一次发消息时生成，之后不变，保证多次 autoSave 只更新同一条历史记录。
     * 新建对话 / 恢复历史对话时重置。
     */
    private var currentConversationId: Long = System.currentTimeMillis()

    fun sendMessage(userText: String) {
        if (userText.isBlank() || _uiState.value.isLoading) return
        viewModelScope.launch {
            val userMsg = ChatMessage(role = "user", content = userText.trim())
            _messages.update { it + userMsg }
            val assistantId = System.currentTimeMillis() + 1
            _messages.update { it + ChatMessage(id = assistantId, role = "assistant", content = "") }
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val s = settings.value
                val effectiveReasoning = if (isThinkingModel(s.model)) reasoningEffort.value else "none"
                var fullResponse = ""
                chatApi.streamChat(
                    baseUrl         = s.baseUrl,
                    apiKey          = s.apiKey,
                    model           = s.model,
                    systemPrompt    = s.systemPrompt,
                    messages        = _messages.value.dropLast(1),
                    reasoningEffort = effectiveReasoning,
                    onDelta         = { delta ->
                        fullResponse += delta
                        _messages.update { msgs ->
                            msgs.map { if (it.id == assistantId) it.copy(content = fullResponse) else it }
                        }
                    }
                )
            } catch (e: Exception) {
                _messages.update { it.filter { msg -> msg.id != assistantId } }
                _uiState.update { it.copy(error = "发送失败: ${e.message?.take(50)}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun isThinkingModel(model: String): Boolean {
        val m = model.lowercase()
        return m.contains("r1") || m.contains("o1") || m.contains("o3") ||
               m.contains("qwq") || m.contains("thinking") || m.contains("reasoner")
    }

    fun fetchModels(currentUrl: String, currentKey: String) {
        if (currentKey.isBlank()) { _uiState.update { it.copy(error = "请先填写 API Key") }; return }
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingModels = true, error = null) }
            _availableModels.value = emptyList()
            try {
                val models = chatApi.fetchModels(currentUrl.trim(), currentKey.trim())
                if (models.isEmpty()) _uiState.update { it.copy(error = "未找到可用模型") }
                else _availableModels.value = models
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "获取失败: ${e.message?.take(40)}") }
            } finally {
                _uiState.update { it.copy(isFetchingModels = false) }
            }
        }
    }

    fun importFromTxt(content: String): Triple<String, String, String>? {
        val lines = content.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.size < 2) return null
        if (!lines[0].startsWith("http")) return null
        return Triple(lines[0], lines[1], if (lines.size >= 3) lines[2] else settings.value.model)
    }

    fun importPromptFromTxt(content: String): String = content.trim()

    fun saveProviderConfig(name: String, url: String, key: String, model: String) {
        if (name.isBlank() || url.isBlank() || key.isBlank()) return
        viewModelScope.launch {
            prefsRepo.saveProviderConfig(ProviderConfig(name = name, baseUrl = url, apiKey = key, model = model))
        }
    }

    fun deleteProviderConfig(id: Long) {
        viewModelScope.launch { prefsRepo.deleteProviderConfig(id) }
    }

    fun setFontSize(size: Int) {
        viewModelScope.launch { prefsRepo.saveFontSize(size) }
    }

    fun setReasoningEffort(effort: String) {
        viewModelScope.launch { prefsRepo.saveReasoningEffort(effort) }
    }

    /** 保存当前对话到历史，开始新对话，重置 conversationId */
    fun newConversation() {
        viewModelScope.launch {
            prefsRepo.saveConversation(_messages.value, currentConversationId)
            _messages.value = emptyList()
            currentConversationId = System.currentTimeMillis()  // 新对话新ID
        }
    }

    /** 从历史恢复对话，使用该历史条目的 ID，后续追加的消息保存回同一条 */
    fun restoreConversation(snapshot: ConversationSnapshot) {
        _messages.value = snapshot.messages.map {
            ChatMessage(id = it.id, role = it.role, content = it.content)
        }
        currentConversationId = snapshot.id   // 恢复后继续使用同一 ID
        navigateTo(Screen.CHAT)
    }

    fun deleteConversation(id: Long) {
        viewModelScope.launch { prefsRepo.deleteConversation(id) }
    }

    fun saveSettings(newSettings: AppSettings) {
        viewModelScope.launch { prefsRepo.saveSettings(newSettings) }
    }

    fun navigateTo(screen: Screen) {
        _uiState.update { it.copy(screen = screen, error = null) }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }

    /** onStop / onCleared 调用，用稳定 ID 保存，不会产生重复条目 */
    fun autoSave() {
        val current = _messages.value
        if (current.size >= 2) {
            viewModelScope.launch {
                prefsRepo.saveConversation(current, currentConversationId)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoSave()
    }
}

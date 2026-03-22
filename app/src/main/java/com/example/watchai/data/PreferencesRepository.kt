package com.example.watchai.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "watchai_settings")

data class AppSettings(
    val baseUrl: String      = "https://api.openai.com/v1",
    val apiKey: String       = "",
    val model: String        = "gpt-4o-mini",
    val systemPrompt: String = "你是手表上的AI助手，回答要简洁，因为屏幕很小。"
)

@Serializable
data class ProviderConfig(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val model: String
)

@Serializable
data class ConversationSnapshot(
    val id: Long = System.currentTimeMillis(),
    val title: String = "对话",
    val messages: List<SerializableMessage>
)

@Serializable
data class SerializableMessage(val id: Long, val role: String, val content: String)

class PreferencesRepository(private val context: Context) {

    companion object {
        private val KEY_BASE_URL       = stringPreferencesKey("base_url")
        private val KEY_API_KEY        = stringPreferencesKey("api_key")
        private val KEY_MODEL          = stringPreferencesKey("model")
        private val KEY_SYSTEM_PROMPT  = stringPreferencesKey("system_prompt")
        private val KEY_HISTORY        = stringPreferencesKey("conversation_history")
        private val KEY_PROVIDERS      = stringPreferencesKey("provider_configs")
        private val KEY_FONT_SIZE      = intPreferencesKey("font_size")
        private val KEY_REASONING      = stringPreferencesKey("reasoning_effort") // none/low/medium/high
    }

    private val json = Json { ignoreUnknownKeys = true }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            baseUrl      = prefs[KEY_BASE_URL]      ?: "https://api.openai.com/v1",
            apiKey       = prefs[KEY_API_KEY]       ?: "",
            model        = prefs[KEY_MODEL]         ?: "gpt-4o-mini",
            systemPrompt = prefs[KEY_SYSTEM_PROMPT] ?: "你是手表上的AI助手。回答时尽量不使用Markdown格式，用自然语言表达。"
        )
    }

    val fontSizeFlow: Flow<Int> = context.dataStore.data.map { it[KEY_FONT_SIZE] ?: 13 }

    val reasoningEffortFlow: Flow<String> = context.dataStore.data.map {
        it[KEY_REASONING] ?: "none"
    }

    val historyFlow: Flow<List<ConversationSnapshot>> = context.dataStore.data.map { prefs ->
        try { json.decodeFromString(prefs[KEY_HISTORY] ?: "[]") }
        catch (_: Exception) { emptyList() }
    }

    val providersFlow: Flow<List<ProviderConfig>> = context.dataStore.data.map { prefs ->
        try { json.decodeFromString(prefs[KEY_PROVIDERS] ?: "[]") }
        catch (_: Exception) { emptyList() }
    }

    suspend fun saveSettings(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BASE_URL]      = settings.baseUrl
            prefs[KEY_API_KEY]       = settings.apiKey
            prefs[KEY_MODEL]         = settings.model
            prefs[KEY_SYSTEM_PROMPT] = settings.systemPrompt
        }
    }

    suspend fun saveFontSize(size: Int) {
        context.dataStore.edit { it[KEY_FONT_SIZE] = size.coerceIn(11, 18) }
    }

    suspend fun saveReasoningEffort(effort: String) {
        context.dataStore.edit { it[KEY_REASONING] = effort }
    }

    /**
     * 保存或更新对话到历史
     * @param conversationId 稳定 ID，同一对话多次调用只更新不新增
     */
    suspend fun saveConversation(messages: List<ChatMessage>, conversationId: Long) {
        if (messages.size < 2) return
        val title = (messages.firstOrNull { it.role == "user" }?.content ?: "对话")
            .take(12).let { if (it.length == 12) "$it…" else it }
        val snapshot = ConversationSnapshot(
            id       = conversationId,   // 使用稳定 ID，不再每次 currentTimeMillis
            title    = title,
            messages = messages.map { SerializableMessage(it.id, it.role, it.content) }
        )
        context.dataStore.edit { prefs ->
            val existing = try {
                json.decodeFromString<List<ConversationSnapshot>>(prefs[KEY_HISTORY] ?: "[]")
            } catch (_: Exception) { emptyList() }
            // upsert：有相同 ID 则替换，没有则插入头部，最多保留 10 条
            val updated = listOf(snapshot) + existing.filter { it.id != conversationId }
            prefs[KEY_HISTORY] = json.encodeToString(updated.take(10))
        }
    }

    suspend fun deleteConversation(id: Long) {
        context.dataStore.edit { prefs ->
            val existing = try { json.decodeFromString<List<ConversationSnapshot>>(prefs[KEY_HISTORY] ?: "[]") }
                           catch (_: Exception) { emptyList() }
            prefs[KEY_HISTORY] = json.encodeToString(existing.filter { it.id != id })
        }
    }

    suspend fun saveProviderConfig(config: ProviderConfig) {
        context.dataStore.edit { prefs ->
            val existing = try { json.decodeFromString<List<ProviderConfig>>(prefs[KEY_PROVIDERS] ?: "[]") }
                           catch (_: Exception) { emptyList() }
            prefs[KEY_PROVIDERS] = json.encodeToString(
                (listOf(config) + existing.filter { it.name != config.name }).take(10)
            )
        }
    }

    suspend fun deleteProviderConfig(id: Long) {
        context.dataStore.edit { prefs ->
            val existing = try { json.decodeFromString<List<ProviderConfig>>(prefs[KEY_PROVIDERS] ?: "[]") }
                           catch (_: Exception) { emptyList() }
            prefs[KEY_PROVIDERS] = json.encodeToString(existing.filter { it.id != id })
        }
    }
}

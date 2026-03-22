package com.example.watchai.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val role: String,
    val content: String
)

class ChatApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 流式聊天
     * @param reasoningEffort 思考强度：none/low/medium/high
     *   - none：不传该参数，普通模型正常工作
     *   - low/medium/high：传给支持思考的模型（DeepSeek-R1、o1 等）
     */
    suspend fun streamChat(
        baseUrl: String,
        apiKey: String,
        model: String,
        systemPrompt: String,
        messages: List<ChatMessage>,
        reasoningEffort: String = "none",
        onDelta: suspend (String) -> Unit
    ) {
        val jsonMessages = buildJsonArray {
            if (systemPrompt.isNotBlank()) {
                addJsonObject { put("role", "system"); put("content", systemPrompt) }
            }
            messages.forEach { msg ->
                addJsonObject { put("role", msg.role); put("content", msg.content) }
            }
        }

        val body = buildJsonObject {
            put("model", model)
            put("stream", true)
            put("max_tokens", 1000)
            put("messages", jsonMessages)
            // 只有非 none 时才传思考参数，避免普通模型报错
            if (reasoningEffort != "none") {
                put("reasoning_effort", reasoningEffort)  // DeepSeek-R1 / 通义 QwQ 格式
            }
        }.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/chat/completions")
            .post(body)
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "text/event-stream")
            .build()

        withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: "未知错误"
                    throw Exception("API ${response.code}: $err")
                }
                val source = response.body?.source() ?: throw Exception("响应体为空")
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    if (!line.startsWith("data: ")) continue
                    val data = line.removePrefix("data: ").trim()
                    if (data == "[DONE]" || data.isBlank()) continue
                    try {
                        val obj = json.parseToJsonElement(data).jsonObject
                        val choice = obj["choices"]?.jsonArray?.getOrNull(0)?.jsonObject ?: continue
                        val delta = choice["delta"]?.jsonObject ?: continue
                        // 跳过 reasoning_content（思考过程），只取 content（最终回答）
                        val content = delta["content"]?.jsonPrimitive?.contentOrNull ?: continue
                        if (content.isNotEmpty()) withContext(Dispatchers.Main) { onDelta(content) }
                    } catch (_: Exception) { }
                }
            }
        }
    }

    suspend fun fetchModels(baseUrl: String, apiKey: String): List<String> {
        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/models")
            .get()
            .header("Authorization", "Bearer $apiKey")
            .build()
        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("获取模型失败 ${response.code}")
                val body = response.body?.string() ?: throw Exception("响应为空")
                val parsed = json.parseToJsonElement(body).jsonObject
                val arr = parsed["data"]?.jsonArray ?: parsed["models"]?.jsonArray ?: return@use emptyList()
                arr.mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.contentOrNull }
                    .filter { it.isNotBlank() }.sorted()
            }
        }
    }
}

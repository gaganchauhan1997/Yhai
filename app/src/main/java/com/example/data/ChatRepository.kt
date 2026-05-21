package com.example.data

import com.example.network.GeminiCandidate
import com.example.network.GeminiContent
import com.example.network.GeminiGenerateContentRequest
import com.example.network.GeminiPart
import com.example.network.GroqChatRequest
import com.example.network.GroqMessage
import com.example.network.RetrofitClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ChatRepository(
    private val apiKeyDao: ApiKeyDao,
    private val messageDao: MessageDao
) {
    val allMessages: Flow<List<MessageEntity>> = messageDao.getAllMessages()
    val allApiKeys: Flow<List<ApiKeyEntity>> = apiKeyDao.getAllApiKeys()

    suspend fun getActiveApiKey(): ApiKeyEntity? {
        return apiKeyDao.getActiveApiKey()
    }

    suspend fun insertApiKey(provider: String, key: String) {
        val count = apiKeyDao.getAllApiKeys().first().size
        val entity = ApiKeyEntity(provider, key, isActive = count == 0) // Make active if first
        apiKeyDao.insertApiKey(entity)
    }

    suspend fun setActiveProvider(provider: String) {
        apiKeyDao.deactivateAll()
        apiKeyDao.activateProvider(provider)
    }

    suspend fun deleteApiKey(provider: String) {
        apiKeyDao.deleteApiKey(provider)
    }

    suspend fun insertMessage(role: String, text: String) {
        messageDao.insertMessage(MessageEntity(role = role, text = text))
    }

    suspend fun clearHistory() {
        messageDao.clearHistory()
    }

    suspend fun sendMessage(prompt: String): String {
        val activeKey = apiKeyDao.getActiveApiKey()
            ?: return "Error: No active API Key found! Please configure one in Settings."

        // Insert User Message into DB
        insertMessage("user", prompt)

        return try {
            val responseText = if (activeKey.provider.equals("Gemini", ignoreCase = true)) {
                sendToGemini(prompt, activeKey.apiKey)
            } else if (activeKey.provider.equals("Groq", ignoreCase = true)) {
                sendToGroq(prompt, activeKey.apiKey)
            } else {
                "Error: Unknown provider ${activeKey.provider}"
            }
            
            // Insert Model Response into DB
            insertMessage("model", responseText)
            
            responseText
        } catch (e: Exception) {
            val errorText = "Error: ${e.message}"
            insertMessage("model", errorText)
            errorText
        }
    }

    private suspend fun sendToGemini(prompt: String, apiKey: String): String {
        // Build context from previous messages
        val history = messageDao.getAllMessages().first().takeLast(10) // Let's take the last 10
        val contents = history.map { 
           // Gemini expects 'user' and 'model'
           val role = if (it.role == "user") "user" else "model"
           GeminiContent(role = role, parts = listOf(GeminiPart(text = it.text)))
        }

        val request = GeminiGenerateContentRequest(
            systemInstruction = GeminiContent(role = "system", parts = listOf(GeminiPart(text = "You are Y-HAI, a secure and highly capable AI Workforce agent."))),
            contents = contents
        )

        val response = RetrofitClient.geminiService.generateContent("gemini-3.5-flash", apiKey, request)
        return response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Empty response"
    }

    private suspend fun sendToGroq(prompt: String, apiKey: String): String {
        // Groq/OpenAI expects 'system', 'user', 'assistant'
        val history = messageDao.getAllMessages().first().takeLast(10)
        val messages = mutableListOf<GroqMessage>()
        messages.add(GroqMessage(role = "system", content = "You are Y-HAI, a secure and highly capable AI Workforce agent."))
        history.forEach { 
             val role = if (it.role == "user") "user" else "assistant"
             messages.add(GroqMessage(role = role, content = it.text))
        }

        val request = GroqChatRequest(
            model = "llama-3.1-8b-instant",
            messages = messages
        )

        val response = RetrofitClient.groqService.chatCompletions("Bearer $apiKey", request)
        return response.choices?.firstOrNull()?.message?.content ?: "Empty response"
    }
}

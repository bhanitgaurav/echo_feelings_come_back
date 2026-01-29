package com.bhanit.apps.echo.features.settings.data

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json

class ContentApi(private val client: HttpClient, private val baseUrl: String) {
    suspend fun getContent(type: String): String {
        val sanitizedBase = baseUrl.trimEnd('/')
        val endpoint = "content/${type.uppercase()}"
        val url = "$sanitizedBase/$endpoint"
        
        val response = client.get(url)
        val jsonString = response.bodyAsText()
        // Response is expected to be {"content": "..."}
        // Simple manual parsing or use serialization if response is complex
        val json = Json { ignoreUnknownKeys = true }
        val map = json.decodeFromString<Map<String, String>>(jsonString)
        return map["content"] ?: ""
    }
}

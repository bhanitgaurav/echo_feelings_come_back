package com.bhanit.apps.echo.features.messaging.data

import com.bhanit.apps.echo.features.messaging.domain.Emotion
import com.bhanit.apps.echo.features.messaging.domain.EmotionRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class EmotionRepositoryImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String
) : EmotionRepository {
    
    // In-memory cache
    private val _emotions = MutableStateFlow<List<Emotion>>(emptyList())
    private val mutex = Mutex()
    private var loaded = false

    override suspend fun getEmotions(forceRefresh: Boolean): List<Emotion> {
        return mutex.withLock {
            if (loaded && !forceRefresh && _emotions.value.isNotEmpty()) {
                return@withLock _emotions.value
            }
            
            try {
                // Fetch from API
                val response: List<Emotion> = httpClient.get("$baseUrl/emotions").body()
                _emotions.value = response
                loaded = true
                response
            } catch (e: Exception) {
                e.printStackTrace()
                // Return cache if available, or empty
                _emotions.value
            }
        }
    }

    override suspend fun getEmotionById(id: String): Emotion? {
        if (!loaded) {
            getEmotions()
        }
        return _emotions.value.find { it.id == id } 
            ?: createFallbackEmotion(id) // Graceful fallback
    }
    
    private fun createFallbackEmotion(id: String): Emotion {
        // Fallback for unknown IDs (e.g. from history before sync)
        return Emotion(
            id = id,
            displayName = id.lowercase().replaceFirstChar { it.uppercase() },
            colorHex = 0xFF888888, // Gray
            exampleText = "",
            isDefault = false,
            isUnlocked = true
        )
    }
}

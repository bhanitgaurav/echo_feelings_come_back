package com.bhanit.apps.echo.features.messaging.domain

interface EmotionRepository {
    suspend fun getEmotions(forceRefresh: Boolean = false): List<Emotion>
    suspend fun getEmotionById(id: String): Emotion?
}

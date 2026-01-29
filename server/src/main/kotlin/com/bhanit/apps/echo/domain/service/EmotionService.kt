package com.bhanit.apps.echo.domain.service

import com.bhanit.apps.echo.data.model.Emotion
import com.bhanit.apps.echo.data.repository.EmotionRepository
import java.util.UUID

class EmotionService(private val emotionRepository: EmotionRepository) {
    
    suspend fun getEmotionsForUser(userId: UUID): List<Emotion> {
        return emotionRepository.getEmotionsForUser(userId)
    }

    suspend fun seedDefaults() {
        emotionRepository.seedDefaultEmotions()
    }
}

package com.bhanit.apps.echo.features.settings.domain

import com.bhanit.apps.echo.features.settings.data.ContentApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ContentRepository(private val api: ContentApi) {
    // We can add simple memory cache here if needed
    
    fun getContent(type: String): Flow<String> = flow {
         val content = api.getContent(type)
         emit(content)
    }
}

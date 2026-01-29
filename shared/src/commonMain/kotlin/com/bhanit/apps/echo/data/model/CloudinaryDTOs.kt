package com.bhanit.apps.echo.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CloudinaryParamsDTO(
    val apiKey: String,
    val cloudName: String,
    val timestamp: Long,
    val signature: String,
    val publicId: String,
    val folder: String? = null,
    val overwrite: Boolean = false
)

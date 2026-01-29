package com.bhanit.apps.echo.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CloudinaryResponse(
    val public_id: String? = null,
    val secure_url: String? = null,
    val error: CloudinaryError? = null
)

@Serializable
data class CloudinaryError(
    val message: String
)

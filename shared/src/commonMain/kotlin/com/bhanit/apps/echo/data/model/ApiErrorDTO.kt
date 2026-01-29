package com.bhanit.apps.echo.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiErrorDTO(
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val errorDescription: String? = null
)

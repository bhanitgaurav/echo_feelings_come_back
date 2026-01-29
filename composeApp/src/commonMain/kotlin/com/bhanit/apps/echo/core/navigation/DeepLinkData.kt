package com.bhanit.apps.echo.core.navigation

data class DeepLinkData(
    val type: String?,
    val userId: String? = null,
    val username: String? = null,
    val connectionId: String? = null,
    val referralCode: String? = null,
    val navigateTo: String? = null,
    val extras: Map<String, String> = emptyMap()
)

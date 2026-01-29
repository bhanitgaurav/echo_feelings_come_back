package com.bhanit.apps.echo.core.navigation

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

fun handleDeepLinkFromSwift(
    type: String?, 
    userId: String?, 
    username: String?, 
    connectionId: String?, 
    referralCode: String?,
    navigateTo: String? = null
) {
    MainScope().launch {
        DeepLinkManager.handleDeepLink(
            DeepLinkData(
                type = type,
                userId = userId,
                username = username,
                connectionId = connectionId,
                referralCode = referralCode,
                navigateTo = navigateTo
            )
        )
    }
}

package com.bhanit.apps.echo.core.permission

import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.Foundation.NSURL
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class IosPermissionHandler : PermissionHandler {
    override suspend fun hasNotificationPermission(): Boolean = suspendCoroutine { continuation ->
        UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler { settings ->
            val status = settings?.authorizationStatus
             continuation.resume(status == UNAuthorizationStatusAuthorized || status == UNAuthorizationStatusProvisional || status == UNAuthorizationStatusEphemeral)
        }
    }

    override suspend fun getPermissionStatus(): PermissionStatus = suspendCoroutine { continuation ->
        UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler { settings ->
            val status = settings?.authorizationStatus
            val result = when (status) {
                UNAuthorizationStatusAuthorized, 
                UNAuthorizationStatusProvisional, 
                UNAuthorizationStatusEphemeral -> PermissionStatus.GRANTED
                UNAuthorizationStatusDenied -> PermissionStatus.DENIED_FOREVER
                UNAuthorizationStatusNotDetermined -> PermissionStatus.NOT_DETERMINED
                else -> PermissionStatus.NOT_DETERMINED
            }
            continuation.resume(result)
        }
    }

    override suspend fun requestNotificationPermission() {
        // iOS doesn't need "hasAsked" check for rationale like Android, 
        // but if it's already denied, requesting again does nothing. 
        // This method is called when we want to trigger the system prompt.
        val center = UNUserNotificationCenter.currentNotificationCenter()
        center.requestAuthorizationWithOptions(
            options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        ) { granted, error ->
            // Handle completion if needed
        }
    }

    override fun openAppSettings() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString)
        if (url != null && UIApplication.sharedApplication.canOpenURL(url)) {
            // Use openURL with options (this maps to open(_:options:completionHandler:))
            UIApplication.sharedApplication.openURL(url, mapOf<Any?, Any?>(), null)
        }
    }
}

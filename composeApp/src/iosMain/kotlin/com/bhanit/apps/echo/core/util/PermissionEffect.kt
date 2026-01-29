package com.bhanit.apps.echo.core.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import platform.Contacts.CNContactStore
import platform.Contacts.CNEntityType
import platform.Contacts.CNAuthorizationStatusAuthorized
import platform.Contacts.CNAuthorizationStatusNotDetermined

@Composable
actual fun ContactPermissionEffect(
    onPermissionResult: (Boolean) -> Unit
) {
    LaunchedEffect(Unit) {
        val store = CNContactStore()
        // Attempting to access CNEntityTypeContacts via CNEntityType companion or static member if available
        // If this fails, we might need to look for the top-level constant mapping.
        // Assuming CNEntityType.CNEntityTypeContacts based on K/N mapping patterns for conflicting names.
        val entityType = CNEntityType.CNEntityTypeContacts 
        
        val status = CNContactStore.authorizationStatusForEntityType(entityType)
        
        when (status) {
            CNAuthorizationStatusAuthorized -> {
                onPermissionResult(true)
            }
            CNAuthorizationStatusNotDetermined -> {
                store.requestAccessForEntityType(entityType) { granted, _ ->
                    onPermissionResult(granted)
                }
            }
            else -> {
                onPermissionResult(false)
            }
        }
    }
}

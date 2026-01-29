package com.bhanit.apps.echo.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Contacts.CNContactStore
import platform.Contacts.CNEntityType
import platform.Contacts.CNAuthorizationStatusAuthorized
import platform.Contacts.CNAuthorizationStatusNotDetermined
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

@Composable
actual fun rememberContactPermissionLauncher(onResult: (Boolean) -> Unit): PermissionLauncher {
    return remember {
        object : PermissionLauncher {
            override fun launch() {
                val store = CNContactStore()
                val status = CNContactStore.authorizationStatusForEntityType(CNEntityType.CNEntityTypeContacts)
                
                if (status == CNAuthorizationStatusAuthorized) {
                    onResult(true)
                } else if (status == CNAuthorizationStatusNotDetermined) {
                    store.requestAccessForEntityType(CNEntityType.CNEntityTypeContacts) { granted, _ ->
                        // Switch to main thread if needed, though onResult usually just updates state
                        MainScope().launch {
                            onResult(granted)
                        }
                    }
                } else {
                    onResult(false)
                }
            }
        }
    }
}

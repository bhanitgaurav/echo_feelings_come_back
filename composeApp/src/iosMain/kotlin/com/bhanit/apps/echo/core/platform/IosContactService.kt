package com.bhanit.apps.echo.core.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Contacts.*
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class IosContactService : PlatformContactService {
    @OptIn(ExperimentalForeignApi::class)
    override suspend fun getContacts(): List<Contact> = suspendCoroutine { continuation ->
        val store = CNContactStore()
        store.requestAccessForEntityType(CNEntityType.CNEntityTypeContacts) { granted, error ->
            if (granted) {
                val keys = listOf(
                    CNContactGivenNameKey,
                    CNContactFamilyNameKey,
                    CNContactPhoneNumbersKey,
                    CNContactThumbnailImageDataKey
                )
                val request = CNContactFetchRequest(keysToFetch = keys)
                val contacts = mutableListOf<Contact>()

                try {
                    store.enumerateContactsWithFetchRequest(request, error = null) { cnContact, _ ->
                         val name = "${cnContact?.givenName} ${cnContact?.familyName}".trim()
                         val phoneNumbers = cnContact?.phoneNumbers?.mapNotNull { 
                             (it as? CNLabeledValue)?.value as? CNPhoneNumber 
                         }?.map { it.stringValue } ?: emptyList()

                         if (name.isNotEmpty() && phoneNumbers.isNotEmpty()) {
                             // Simple logic: take the first number (real logic might be more complex)
                             val primaryPhone = phoneNumbers.first()
                                 .replace(" ", "")
                                 .replace("-", "")
                                 .replace("(", "")
                                 .replace(")", "")
                             
                             contacts.add(Contact(name, primaryPhone, primaryPhone)) // Normalizing phone is tricky without libphonenumber
                         }
                    }
                    continuation.resume(contacts)
                } catch (e: Exception) {
                    println("Error fetching contacts: ${e.message}")
                    continuation.resume(emptyList())
                }
            } else {
                continuation.resume(emptyList())
            }
        }
    }

    override fun openWhatsApp(phone: String, message: String) {
        val urlString = "whatsapp://send?phone=$phone&text=${message}"
        val url = NSURL.URLWithString(urlString.replace(" ", "%20"))
        if (url != null && UIApplication.sharedApplication.canOpenURL(url)) {
            UIApplication.sharedApplication.openURL(url)
        } else {
             // Fallback to App Store if WhatsApp not installed? Or just print
             println("WhatsApp not installed or cannot open URL")
        }
    }

    override fun openSms(phone: String, message: String) {
        val urlString = "sms:$phone&body=${message}"
        val url = NSURL.URLWithString(urlString.replace(" ", "%20"))
        if (url != null && UIApplication.sharedApplication.canOpenURL(url)) {
            UIApplication.sharedApplication.openURL(url)
        }
    }
}

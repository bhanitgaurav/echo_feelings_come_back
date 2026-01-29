package com.bhanit.apps.echo.core.platform

import com.bhanit.apps.echo.data.model.UserSummary

interface PlatformContactService {
    suspend fun getContacts(): List<Contact>
    fun openWhatsApp(phone: String, message: String)
    fun openSms(phone: String, message: String)
}

data class Contact(
    val name: String,
    val phone: String,
    val normalizedPhone: String // E.164 format
)

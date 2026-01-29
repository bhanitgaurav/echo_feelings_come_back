package com.bhanit.apps.echo.core.platform

import android.content.Context
import android.content.Intent
import android.net.Uri

class AndroidContactService(private val context: Context) : PlatformContactService {
    override suspend fun getContacts(): List<Contact> {
        // Mock contacts for now. Real implementation requires Runtime Permissions.
        return listOf(
            Contact("Alice", "1234567890", "+11234567890"),
            Contact("Bob", "0987654321", "+10987654321")
        )
    }

    override fun openWhatsApp(phone: String, message: String) {
        try {
            val url = "https://api.whatsapp.com/send?phone=$phone&text=$message"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun openSms(phone: String, message: String) {
         try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.fromParts("sms", phone, null)).apply {
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

package com.bhanit.apps.echo.features.contact.manager

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.bhanit.apps.echo.app.EchoApplication

class AndroidContactManager(private val context: Context) : ContactManager {
    override suspend fun getPermissionStatus(): PermissionStatus {
        val status = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        )
        return if (status == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            PermissionStatus.GRANTED
        } else {
            PermissionStatus.DENIED
        }
    }

    override suspend fun fetchContacts(): List<DeviceContact> {
        if (getPermissionStatus() != PermissionStatus.GRANTED) return emptyList()

        val contacts = mutableListOf<DeviceContact>()
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI // OR PHOTO_THUMBNAIL_URI
            ),
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            // val photoIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

            while (it.moveToNext()) {
                val id = it.getString(idIndex)
                val name = it.getString(nameIndex) ?: "Unknown"
                val number = it.getString(numberIndex) ?: continue
                
                // Basic normalization (remove spaces, etc. - backend logic handles strict parsing but good to clean)
                val normalizedNumber = com.bhanit.apps.echo.core.util.PhoneNumberUtils.normalizePhoneNumber(number)

                contacts.add(
                    DeviceContact(
                        id = id,
                        name = name,
                        phoneNumber = normalizedNumber
                    )
                )
            }
        }
        return contacts.distinctBy { it.phoneNumber } // Remove duplicates
    }

    override fun openSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

actual fun createContactManager(): ContactManager {
    return AndroidContactManager(EchoApplication.INSTANCE)
}

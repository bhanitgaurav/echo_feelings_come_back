package com.bhanit.apps.echo.core.permission

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bhanit.apps.echo.MainActivity
import com.bhanit.apps.echo.core.util.CurrentActivityHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidPermissionHandler : PermissionHandler {

    override suspend fun hasNotificationPermission(): Boolean {
        val activity = CurrentActivityHolder.currentActivity ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            return ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }
        return true // Implicitly granted below Android 13
    }

    override suspend fun getPermissionStatus(): PermissionStatus {
        val activity = CurrentActivityHolder.currentActivity ?: return PermissionStatus.NOT_DETERMINED
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return PermissionStatus.GRANTED
        }

        val permission = android.Manifest.permission.POST_NOTIFICATIONS
        val isGranted = ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        if (isGranted) return PermissionStatus.GRANTED

        // Check if we have asked before
        val prefs = activity.getSharedPreferences("permission_prefs", Context.MODE_PRIVATE)
        val hasAsked = prefs.getBoolean("has_asked_notification", false)

        val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)

        return if (shouldShowRationale) {
            // User denied once but allows asking again
            PermissionStatus.NOT_DETERMINED 
        } else {
            if (hasAsked) {
                // User denied and selected "Done ask again" OR denied multiple times
                PermissionStatus.DENIED_FOREVER
            } else {
                // Never asked
                PermissionStatus.NOT_DETERMINED
            }
        }
    }

    override suspend fun requestNotificationPermission() {
        if (hasNotificationPermission()) return
        
        val activity = CurrentActivityHolder.currentActivity
        
        // Mark as asked
        activity?.getSharedPreferences("permission_prefs", Context.MODE_PRIVATE)
            ?.edit()
            ?.putBoolean("has_asked_notification", true)
            ?.apply()

        withContext(Dispatchers.Main) {
            if (activity is MainActivity) {
                activity.requestNotificationPermission()
            }
        }
    }

    override fun openAppSettings() {
        val activity = CurrentActivityHolder.currentActivity ?: return
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        activity.startActivity(intent)
    }
}

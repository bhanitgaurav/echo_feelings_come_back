package com.bhanit.apps.echo

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.core.view.WindowCompat
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.toArgb
import com.bhanit.apps.echo.core.base.BiometricActivityProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private var requestPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()


        BiometricActivityProvider.setActivity(this)
        
        // Initialize Permission Launcher but DO NOT launch it here
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                     // Permission granted
                }
            }
        }
        
        // ... setContent ...

        setContent {
            val deepLinkData = rememberDeepLinkData(intent)
            App(deepLinkData = deepLinkData)
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        
        // Handle new intent deep link
        // Handle new intent deep link
        val type = intent.getStringExtra("type")
        val userId = intent.getStringExtra("userId")
        val username = intent.getStringExtra("username")
        val connectionId = intent.getStringExtra("connectionId")
        
        val referralCode = intent.getStringExtra("referralCode") 
            ?: intent.getStringExtra("code")
            ?: intent.data?.getQueryParameter("code")
            ?: intent.data?.getQueryParameter("referral_code")

        val navigateTo = intent.getStringExtra("navigate_to") ?: intent.getStringExtra("navigateTo")
        // Note: extras extraction logic
        
        if (type != null || referralCode != null || navigateTo != null) {
            val data = com.bhanit.apps.echo.core.navigation.DeepLinkData(type, userId, username, connectionId, referralCode, navigateTo)
            lifecycleScope.launch {
                com.bhanit.apps.echo.core.navigation.DeepLinkManager.handleDeepLink(data)
            }
        }
    }

    @Composable
    private fun rememberDeepLinkData(intent: android.content.Intent): com.bhanit.apps.echo.core.navigation.DeepLinkData? {
        // We use remember to keep the data stable, but we might want to react to new intents.
        // Actually, onNewIntent updates the activity intent. We should observe it.
        // For simplicity, let's just parse the current intent. 
        // In a real app, we might want a DisposableEffect or a proper Intent handler in ViewModel.
        // But for this requirement, let's try to extract it.
        
        val type = intent.getStringExtra("type")
        val userId = intent.getStringExtra("userId")
        val username = intent.getStringExtra("username")
        val connectionId = intent.getStringExtra("connectionId")
        
        // Check Extras first, then URI Query Params
        val referralCode = intent.getStringExtra("referralCode") 
            ?: intent.getStringExtra("code")
            ?: intent.data?.getQueryParameter("code")
            ?: intent.data?.getQueryParameter("referral_code")

        val navigateTo = intent.getStringExtra("navigate_to") ?: intent.getStringExtra("navigateTo")

        return if (type != null || referralCode != null || navigateTo != null) {
            com.bhanit.apps.echo.core.navigation.DeepLinkData(type, userId, username, connectionId, referralCode, navigateTo)
        } else {
            null
        }
    }
    
    override fun onResume() {
        super.onResume()
        BiometricActivityProvider.setActivity(this)
    }
    
    override fun onPause() {
        super.onPause()
        // Ideally we shouldn't clear it immediately if we want to survive config changes or backgrounding briefly,
        // but for safety let's keep it updated. WeakReference handles leaks.
    }

    fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher?.launch(permission)
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(deepLinkData = null)
}
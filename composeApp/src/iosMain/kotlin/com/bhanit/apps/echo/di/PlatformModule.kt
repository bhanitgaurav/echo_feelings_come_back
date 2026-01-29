package com.bhanit.apps.echo.di

import com.bhanit.apps.echo.core.base.IOSDataStoreFactory
import com.bhanit.apps.echo.core.base.BiometricManager
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { IOSDataStoreFactory().create() }
    single { BiometricManager() }
    single<com.bhanit.apps.echo.core.platform.PlatformContactService> { com.bhanit.apps.echo.core.platform.IosContactService() }
    
    // Placeholder for iOS FCM Token Provider
    single<com.bhanit.apps.echo.core.fcm.FcmTokenProvider> {
        object : com.bhanit.apps.echo.core.fcm.FcmTokenProvider {
            override suspend fun getToken(): String? = null
        }
    }
    
    single<com.bhanit.apps.echo.core.fcm.NotificationHelper> {
        object : com.bhanit.apps.echo.core.fcm.NotificationHelper {
            override fun cancelAll() {
                platform.UserNotifications.UNUserNotificationCenter.currentNotificationCenter().removeAllDeliveredNotifications()
            }
        }
    }
    single<com.bhanit.apps.echo.core.util.PlatformUtils> { com.bhanit.apps.echo.core.util.IosPlatformUtils() }
    single<com.bhanit.apps.echo.core.util.ShareManager> { com.bhanit.apps.echo.core.util.IosShareManager() }
    single<com.bhanit.apps.echo.core.utils.ImageCompressor> { com.bhanit.apps.echo.core.utils.IosImageCompressor() }
    single<com.bhanit.apps.echo.core.permission.PermissionHandler> { com.bhanit.apps.echo.core.permission.IosPermissionHandler() }
    single<com.bhanit.apps.echo.core.config.ConfigService> { com.bhanit.apps.echo.core.config.IosConfigService() }
    single<com.bhanit.apps.echo.shared.analytics.EchoAnalytics> { com.bhanit.apps.echo.analytics.IosEchoAnalytics() }
}

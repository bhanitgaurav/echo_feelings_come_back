package com.bhanit.apps.echo.di

import com.bhanit.apps.echo.core.base.AndroidDataStoreFactory
import com.bhanit.apps.echo.core.base.BiometricManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { AndroidDataStoreFactory(androidContext()).create() }
    single { BiometricManager(androidContext()) }
    single<com.bhanit.apps.echo.core.platform.PlatformContactService> { com.bhanit.apps.echo.core.platform.AndroidContactService(androidContext()) }
    single<com.bhanit.apps.echo.core.fcm.FcmTokenProvider> { com.bhanit.apps.echo.core.fcm.AndroidFcmTokenProvider() }
    single<com.bhanit.apps.echo.core.fcm.NotificationHelper> { com.bhanit.apps.echo.core.fcm.AndroidNotificationHelper(androidContext()) }
    single<com.bhanit.apps.echo.core.util.PlatformUtils> { com.bhanit.apps.echo.core.util.AndroidPlatformUtils(androidContext()) }
    single<com.bhanit.apps.echo.core.util.ShareManager> { com.bhanit.apps.echo.core.util.AndroidShareManager(androidContext()) }
    single<com.bhanit.apps.echo.core.utils.ImageCompressor> { com.bhanit.apps.echo.core.utils.AndroidImageCompressor() }
    single<com.bhanit.apps.echo.shared.analytics.EchoAnalytics> { com.bhanit.apps.echo.analytics.AndroidEchoAnalytics(androidContext(), get()) }
    single<com.bhanit.apps.echo.core.config.ConfigService> { com.bhanit.apps.echo.core.config.AndroidConfigService() }
    single<com.bhanit.apps.echo.core.review.ReviewManager> { com.bhanit.apps.echo.core.review.AndroidReviewManager(androidContext()) }
    single<com.bhanit.apps.echo.core.permission.PermissionHandler> { com.bhanit.apps.echo.core.permission.AndroidPermissionHandler() }
}

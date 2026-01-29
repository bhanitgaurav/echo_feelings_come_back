package com.bhanit.apps.echo

import androidx.compose.ui.window.ComposeUIViewController
import com.bhanit.apps.echo.core.config.ConfigService
import com.bhanit.apps.echo.core.fcm.FcmTokenProvider
import com.bhanit.apps.echo.di.appModule
import com.bhanit.apps.echo.shared.analytics.EchoAnalytics
import org.koin.core.context.startKoin
import org.koin.dsl.module

import com.bhanit.apps.echo.core.review.ReviewManager

fun MainViewController(
    analytics: EchoAnalytics, 
    fcmTokenProvider: FcmTokenProvider,
    configService: ConfigService,
    reviewManager: ReviewManager,
    baseUrl: String,
    envName: String,
    version: String
) = ComposeUIViewController {
    startKoin {
        modules(appModule + module {
            single { analytics }
            single { fcmTokenProvider }
            single { configService }
            single { reviewManager }
            single { 
                com.bhanit.apps.echo.core.config.AppConfig(
                    baseUrl = baseUrl,
                    envName = envName,
                    version = version
                ) 
            }
        })
    }
    App(deepLinkData = null)
}
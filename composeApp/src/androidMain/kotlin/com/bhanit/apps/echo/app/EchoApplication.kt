package com.bhanit.apps.echo.app

import android.app.Application
import com.bhanit.apps.echo.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import org.koin.core.context.startKoin

class EchoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
        
        startKoin {
            androidContext(this@EchoApplication)
            androidLogger()
            modules(appModule, org.koin.dsl.module {
                single { 
                    com.bhanit.apps.echo.core.config.AppConfig(
                        baseUrl = com.bhanit.apps.echo.BuildConfig.BASE_URL,
                        envName = com.bhanit.apps.echo.BuildConfig.ENV_NAME,
                        version = "${com.bhanit.apps.echo.BuildConfig.VERSION_NAME} (${com.bhanit.apps.echo.BuildConfig.VERSION_CODE})"
                    ) 
                }
            })
        }

        createNotificationChannels()
        registerActivityLifecycleCallbacks(com.bhanit.apps.echo.core.util.CurrentActivityHolder)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val socialChannel = NotificationChannel(
                "social_messages",
                "Feelings & Replies",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new feelings and replies from connections."
            }

            val checkinsChannel = NotificationChannel(
                "gentle_checkins",
                "Gentle Check-ins",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Reminders to check in with how you're feeling."
            }

            val reflectionsChannel = NotificationChannel(
                "reflections",
                "Reflections",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily reflection prompts and weekly summaries."
            }

            val rewardsChannel = NotificationChannel(
                "rewards",
                "Rewards",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Updates about your credits and rewards."
            }

            val systemChannel = NotificationChannel(
                "system_updates",
                "System Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Important updates about the app and your account."
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannels(
                listOf(
                    socialChannel,
                    checkinsChannel,
                    reflectionsChannel,
                    rewardsChannel,
                    systemChannel
                )
            )
        }
    }
    
    companion object {
        lateinit var INSTANCE: EchoApplication
            private set
    }
}

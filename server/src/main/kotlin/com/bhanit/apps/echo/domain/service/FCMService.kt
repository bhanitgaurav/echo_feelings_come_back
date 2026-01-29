package com.bhanit.apps.echo.domain.service

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.AndroidNotification
import io.ktor.server.config.ApplicationConfig
import java.io.InputStream

object FCMService {
    private var isInitialized = false

    sealed class FCMResult {
        object Success : FCMResult()
        object InvalidToken : FCMResult()
        data class Failure(val error: String) : FCMResult()
        object NotInitialized : FCMResult()
    }

    fun init(config: ApplicationConfig) {
        if (isInitialized) return
        
        try {
            // Priority 1: Check Current Working Directory (Local dev / certain Docker setups)
            var serviceAccountStream: InputStream? = null
            val pathsToCheck = listOf("service-account.json", "/service-account.json")

            for (path in pathsToCheck) {
                val file = java.io.File(path)
                if (file.exists()) {
                    println("INFO: Found service account file at: $path")
                    serviceAccountStream = java.io.FileInputStream(file)
                    break
                }
            }
            
            // Priority 2: Check Classpath (Resources)
            if (serviceAccountStream == null) {
                serviceAccountStream = FCMService::class.java.getResourceAsStream("/service-account.json")
                if (serviceAccountStream != null) println("INFO: Found service account file in classpath config.")
            }

            if (serviceAccountStream == null) {
                 throw IllegalStateException("Firebase service account file not found. Checked: $pathsToCheck and Classpath.")
            }

            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
                .build()

            FirebaseApp.initializeApp(options)
            isInitialized = true
            println("INFO: Firebase Admin SDK initialized successfully.")
        } catch (e: Exception) {
            println("ERROR: Failed to initialize Firebase Admin SDK: ${e.message}")
            e.printStackTrace()
        }
    }

    fun sendNotification(token: String, title: String, body: String, data: Map<String, String> = emptyMap(), platform: String? = null): FCMResult {
        if (!isInitialized) {
            println("WARN: FCM not initialized. Skipping notification to $token")
            return FCMResult.NotInitialized
        }

        return try {
            val messageBuilder = Message.builder()
                .setToken(token)

            val isIos = platform?.contains("ios", ignoreCase = true) == true
            
            if (isIos) {
                // iOS: Needs "notification" block for Alert + Sound
                val notification = com.google.firebase.messaging.Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build()
                    
                val type = data["type"] ?: "GENERAL"
                val apnsConfig = com.google.firebase.messaging.ApnsConfig.builder()
                    .setAps(com.google.firebase.messaging.Aps.builder()
                        .setSound("echo_notification.wav")
                        .setThreadId(type)
                        .build())
                    .build()
                
                messageBuilder.setNotification(notification)
                messageBuilder.setApnsConfig(apnsConfig)
                
                // Add data payload as well
                data.forEach { (k, v) -> messageBuilder.putData(k, v) }
                // Also put title/body in data for consistency if app handles it in foreground
                messageBuilder.putData("title", title)
                messageBuilder.putData("body", body)
                
            } else {
                // Android: Data-Only + High Priority
                val androidConfig = AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .build()
                    
                messageBuilder.setAndroidConfig(androidConfig)
                
                messageBuilder.putData("title", title)
                messageBuilder.putData("body", body)
                data.forEach { (k, v) -> messageBuilder.putData(k, v) }
            }

            val response = FirebaseMessaging.getInstance().send(messageBuilder.build())
            println("INFO: Successfully sent message via FCM ($platform): $response")
            FCMResult.Success
        } catch (e: com.google.firebase.messaging.FirebaseMessagingException) {
            val errorCode = e.messagingErrorCode
            println("ERROR: FCM Send Failed: ${e.message}, Code: $errorCode")
            
            if (errorCode == com.google.firebase.messaging.MessagingErrorCode.UNREGISTERED || 
                errorCode == com.google.firebase.messaging.MessagingErrorCode.INVALID_ARGUMENT) {
                FCMResult.InvalidToken
            } else {
                FCMResult.Failure(e.message ?: "Unknown FCM Error")
            }
        } catch (e: Exception) {
            println("ERROR: Failed to send FCM message: ${e.message}")
            FCMResult.Failure(e.message ?: "Unknown Error")
        }
    }
}

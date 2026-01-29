package com.bhanit.apps.echo.analytics

import android.content.Context
import android.os.Bundle
import android.net.Uri
import com.bhanit.apps.echo.shared.analytics.EchoAnalytics
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.bhanit.apps.echo.features.auth.domain.SessionRepository
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class AndroidEchoAnalytics(
    context: Context,
    private val sessionRepository: SessionRepository
) : EchoAnalytics {

    private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)
    private val crashlytics = FirebaseCrashlytics.getInstance()

    init {
        // Set Environment User Property
        firebaseAnalytics.setUserProperty("env", com.bhanit.apps.echo.BuildConfig.ENV_NAME)
        trackInstallReferrer(context)
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun trackInstallReferrer(context: Context) {
        val referrerClient = com.android.installreferrer.api.InstallReferrerClient.newBuilder(context).build()
        referrerClient.startConnection(object : com.android.installreferrer.api.InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                if (responseCode == com.android.installreferrer.api.InstallReferrerClient.InstallReferrerResponse.OK) {
                    try {
                        val response = referrerClient.installReferrer
                        val referrerUrl = response.installReferrer
                        
                        // Log the raw referrer string as a user property
                        // The referrer url usually looks like:
                        // utm_source=google-play&utm_medium=organic
                        firebaseAnalytics.setUserProperty("install_referrer", referrerUrl)
                        
                        // Capture Referral Code for Onboarding
                        // Case 1: "referrer=CODE"
                        // Case 2: "utm_source=...&referral_code=CODE"
                        // Case 3: Just "CODE" (if user put &referrer=CODE directly)
                        try {
                             var code: String? = null
                             val uri = Uri.parse("?${referrerUrl}") // Prepend ? to parse as query params
                             code = uri.getQueryParameter("referral_code") 
                                 ?: uri.getQueryParameter("referral")
                                 ?: uri.getQueryParameter("invite_code")
                                 ?: uri.getQueryParameter("code")
                             
                             if (code.isNullOrBlank() && !referrerUrl.contains("=")) {
                                 // It might be just the code itself if no params format
                                 code = referrerUrl
                             }
                             
                             if (!code.isNullOrBlank() && code != "google-play" && !code.startsWith("utm_")) {
                                 GlobalScope.launch {
                                     sessionRepository.saveReferralCode(code)
                                     println("DEBUG: AndroidEchoAnalytics - Saved config referral code: $code")
                                 }
                                 // Explicitly log the extracted code
                                 firebaseAnalytics.setUserProperty("referral_code", code)
                             }

                             // Also parse and log standard UTM params from the referrer string if present
                             val source = uri.getQueryParameter("utm_source")
                             val medium = uri.getQueryParameter("utm_medium")
                             val campaign = uri.getQueryParameter("utm_campaign")
                             val content = uri.getQueryParameter("utm_content")
                             
                             if (!source.isNullOrBlank()) firebaseAnalytics.setUserProperty("referrer_source", source)
                             if (!medium.isNullOrBlank()) firebaseAnalytics.setUserProperty("referrer_medium", medium)
                             if (!campaign.isNullOrBlank()) firebaseAnalytics.setUserProperty("referrer_campaign", campaign)
                             if (!content.isNullOrBlank()) firebaseAnalytics.setUserProperty("referrer_content", content)
                        } catch (e: Exception) {
                            println("DEBUG: Failed to parse referral code: ${e.message}")
                        }
                        
                        // We can also parse it if needed, but logging the full string is often enough for Firebase to process or for manual analysis.
                        // Firebase "First Open" attribution usually handles standard UTM parameters automatically if they are in the deep link, 
                        // but for Play Store install referrer, we need to explicitly log it if we want it as a user property visible in all events.
                        
                        // Also log as an event for easier filtering
                        val bundle = Bundle()
                        bundle.putString("referrer_url", referrerUrl)
                        bundle.putLong("referrer_click_timestamp", response.referrerClickTimestampSeconds)
                        bundle.putLong("install_begin_timestamp", response.installBeginTimestampSeconds)
                        firebaseAnalytics.logEvent("install_referrer_detected", bundle)

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                referrerClient.endConnection()
            }

            override fun onInstallReferrerServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        })
    }

    override fun logEvent(name: String, params: Map<String, Any>) {
        val bundle = Bundle()
        params.forEach { (key, value) ->
            when (value) {
                is String -> bundle.putString(key, value)
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is Double -> bundle.putDouble(key, value)
                is Float -> bundle.putFloat(key, value)
                is Boolean -> bundle.putBoolean(key, value)
                else -> bundle.putString(key, value.toString())
            }
        }
        firebaseAnalytics.logEvent(name, bundle)
    }

    override fun setUserId(id: String) {
        firebaseAnalytics.setUserId(id)
        crashlytics.setUserId(id)
    }

    override fun setUserProperty(name: String, value: String) {
        firebaseAnalytics.setUserProperty(name, value)
    }

    override fun logCrash(message: String) {
        crashlytics.log(message)
        // We can also record a non-fatal exception if needed
        // crashlytics.recordException(Exception(message))
    }
}

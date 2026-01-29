package com.bhanit.apps.echo.analytics

import com.bhanit.apps.echo.shared.analytics.EchoAnalytics

class IosEchoAnalytics : EchoAnalytics {
    override fun logEvent(name: String, params: Map<String, Any>) {
        println("IOS_ANALYTICS: Event '$name' with params: $params")
    }

    override fun setUserId(id: String) {
        println("IOS_ANALYTICS: setUserId: $id")
    }

    override fun setUserProperty(name: String, value: String) {
        println("IOS_ANALYTICS: setUserProperty: $name = $value")
    }

    override fun logCrash(message: String) {
        println("IOS_ANALYTICS: logCrash: $message")
    }
}

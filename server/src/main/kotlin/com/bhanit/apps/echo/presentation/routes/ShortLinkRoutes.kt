package com.bhanit.apps.echo.presentation.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.formUrlEncode

fun Route.shortLinkRoutes() {
    get("/r/{code}") {
        val code = call.parameters["code"]
        val mediumParam = call.request.queryParameters["m"]

        if (code.isNullOrBlank()) {
            call.respondRedirect("/invite")
            return@get
        }

        // Heuristic: User Referral Codes are typically Uppercase Alphanumeric only.
        // If the code contains lowercase letters or is not strictly Uppercase Alphanumeric, treat it as a Marketing Link.
        val isUserReferralCode = code.matches(Regex("^[A-Z0-9]+$"))

        if (!isUserReferralCode) {
            val platform = code.lowercase()
        // e.g. instagram -> INSTAGRAM_BIO, aaratai -> AARATAI_BIO
            val referralCode = "${platform.uppercase()}_BIO"
            
            // Allow dynamic tracking without server redeploy
            // We default to 'profile' medium as these are typically bio/profile links
            val websiteUrl = System.getenv("WEBSITE_URL") ?: "http://localhost:5173"
            val destination = "$websiteUrl/invite?code=$referralCode&utm_source=$platform&utm_medium=profile&utm_campaign=bio_link"
            call.respondRedirect(destination)
            return@get
        }

        // Map short 'm' param to full utm_medium
        val medium = when(mediumParam) {
            "settings" -> "refer_earn_screen"
            "invite" -> "invite_user_screen"
            "qr" -> "qr_scan"
            "qr_scan" -> "qr_scan"
            else -> "user_share_generic"
        }

        // Build destination URL with all original parameters
        val websiteUrl = System.getenv("WEBSITE_URL") ?: "http://localhost:5173"
        val params = io.ktor.http.ParametersBuilder().apply {
            append("code", code)
            append("utm_source", "echo_app")
            append("utm_medium", medium)
            append("utm_campaign", "user_referral")
            
            // Pass through other parameters from original request (except 'm' which we processed)
            call.request.queryParameters.forEach { key, values ->
                if (key != "m" && key != "code") {
                    values.forEach { append(key, it) }
                }
            }
        }
        
        call.respondRedirect("$websiteUrl/invite?${params.build().formUrlEncode()}")
    }
}

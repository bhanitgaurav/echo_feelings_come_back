package com.bhanit.apps.echo.core.navigation

import kotlinx.serialization.Serializable

@Serializable
object LoginRoute

@Serializable
data class OtpRoute(val phone: String)

@Serializable
object DashboardRoute

@Serializable
object SplashRoute

@Serializable
object WelcomeRoute

@Serializable
object OnboardingRoute

@Serializable
object PeopleRoute

@Serializable
data class SendEchoRoute(val userId: String, val username: String, val initialTab: Int = 0)

@Serializable
data object EchoesRoute

@Serializable
object SettingsRoute

@Serializable
object ProfileRoute

@Serializable
object ScanQrRoute


@Serializable
object BlockedListRoute

@Serializable
object AvailableContactsRoute



@Serializable
object AboutRoute

@Serializable
object ContactSyncRoute

@Serializable
object MainRoute

@Serializable
data class PoliciesRoute(val type: String)

@Serializable
object AppSettingsRoute

@Serializable
object ReferralRoute

@Serializable
data class CreditPremiumRoute(val initialTab: Int = 0)

@Serializable
object StreaksRoute

@Serializable
object SupportRoute

@Serializable
object CreateSupportRoute

@Serializable
object NotificationSettingsRoute

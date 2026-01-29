package com.bhanit.apps.echo

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.bhanit.apps.echo.core.navigation.AboutRoute
import com.bhanit.apps.echo.core.navigation.AvailableContactsRoute
import com.bhanit.apps.echo.core.navigation.BlockedListRoute
import com.bhanit.apps.echo.core.navigation.ContactSyncRoute
import com.bhanit.apps.echo.core.navigation.CreditPremiumRoute
import com.bhanit.apps.echo.core.navigation.DashboardRoute
import com.bhanit.apps.echo.core.navigation.DeepLinkData
import com.bhanit.apps.echo.core.navigation.EchoesRoute
import com.bhanit.apps.echo.core.navigation.LoginRoute
import com.bhanit.apps.echo.core.navigation.NotificationSettingsRoute
import com.bhanit.apps.echo.core.navigation.OnboardingRoute
import com.bhanit.apps.echo.core.navigation.OtpRoute
import com.bhanit.apps.echo.core.navigation.PeopleRoute
import com.bhanit.apps.echo.core.navigation.PoliciesRoute
import com.bhanit.apps.echo.core.navigation.ProfileRoute
import com.bhanit.apps.echo.core.navigation.ReferralRoute
import com.bhanit.apps.echo.core.navigation.SendEchoRoute
import com.bhanit.apps.echo.core.navigation.ScanQrRoute
import com.bhanit.apps.echo.core.navigation.SettingsRoute
import com.bhanit.apps.echo.core.navigation.SplashRoute
import com.bhanit.apps.echo.core.navigation.StreaksRoute
import com.bhanit.apps.echo.core.theme.EchoTheme
import com.bhanit.apps.echo.core.theme.AppTheme
import com.bhanit.apps.echo.core.theme.ThemeConfig
import com.bhanit.apps.echo.core.theme.AppFont
import com.bhanit.apps.echo.core.navigation.WelcomeRoute
import com.bhanit.apps.echo.features.welcome.WelcomeScreen
import com.bhanit.apps.echo.features.auth.presentation.login.LoginScreen
import com.bhanit.apps.echo.features.auth.presentation.otp.OtpScreen
import com.bhanit.apps.echo.features.contact.presentation.list.PeopleScreen
import com.bhanit.apps.echo.features.contact.presentation.sync.ContactSyncScreen
import com.bhanit.apps.echo.features.main.MainScaffold
import com.bhanit.apps.echo.features.messaging.presentation.SendEchoScreen
import com.bhanit.apps.echo.features.onboarding.OnboardingScreen
import com.bhanit.apps.echo.features.settings.presentation.about.AboutScreen
import com.bhanit.apps.echo.features.settings.presentation.profile.ProfileScreen
import com.bhanit.apps.echo.features.settings.presentation.settings.NotificationSettingsScreen
import com.bhanit.apps.echo.features.settings.presentation.settings.PoliciesScreen
import com.bhanit.apps.echo.features.splash.SplashScreen
import com.bhanit.apps.echo.core.tutorial.CoachMarkController
import com.bhanit.apps.echo.core.tutorial.CoachMarkOverlay
import com.bhanit.apps.echo.core.tutorial.LocalCoachMarkController
import com.bhanit.apps.echo.core.tutorial.LocalCoachMarkRepository
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App(deepLinkData: DeepLinkData? = null) {
    val navController = rememberNavController()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    
    val sessionRepository = org.koin.compose.koinInject<com.bhanit.apps.echo.features.auth.domain.SessionRepository>()
    val notificationHelper = org.koin.compose.koinInject<com.bhanit.apps.echo.core.fcm.NotificationHelper>()
    val analytics = org.koin.compose.koinInject<com.bhanit.apps.echo.shared.analytics.EchoAnalytics>()
    val userRepository = org.koin.compose.koinInject<com.bhanit.apps.echo.features.user.domain.UserRepository>()
    val coachMarkRepository = org.koin.compose.koinInject<com.bhanit.apps.echo.core.tutorial.CoachMarkRepository>()

    val coachMarkController = remember(coachMarkRepository, scope) { 
        CoachMarkController(coachMarkRepository, scope) 
    }

    val fcmTokenProvider = org.koin.compose.koinInject<com.bhanit.apps.echo.core.fcm.FcmTokenProvider>()
    val updateManager = org.koin.compose.koinInject<com.bhanit.apps.echo.core.config.UpdateManager>()
    val platformUtils = org.koin.compose.koinInject<com.bhanit.apps.echo.core.util.PlatformUtils>()
    
    val updateState = updateManager.updateState.collectAsState().value
    
    LaunchedEffect(Unit) {
        updateManager.checkUpdate()
    }

    LaunchedEffect(Unit) {
        sessionRepository.isLoggedIn.collect { isLoggedIn ->
            if (isLoggedIn) {
                // 1. Sync Device Metadata
                val platform = com.bhanit.apps.echo.getPlatform()
                launch {
                    try {
                        userRepository.syncDeviceDetails(
                            platform = platform.name,
                            appVersion = platform.appVersion,
                            osVersion = platform.osVersion,
                            deviceModel = platform.deviceModel,
                            locale = platform.locale ?: "en",
                            timezone = platform.timezone ?: "UTC"
                        )
                    } catch (e: Exception) { /* Ignore Sync Failures */ }
                }

                // 2. Sync FCM Token (Ensure fresh token on every launch/login)
                launch {
                    try {
                         val token = fcmTokenProvider.getToken()
                         if (!token.isNullOrBlank()) {
                             userRepository.saveFcmToken(token)
                         }
                    } catch (e: Exception) { /* Ignore Token Failures */ }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        com.bhanit.apps.echo.core.navigation.DeepLinkManager.deepLinkEvent.collect { data ->
            analytics.logEvent(com.bhanit.apps.echo.shared.analytics.EchoEvents.NOTIFICATION_OPENED, mapOf(
                com.bhanit.apps.echo.shared.analytics.EchoParams.NOTIFICATION_TYPE to (data.type ?: "unknown")
            ))
            when (data.type) {
                "ECHO" -> {
                    navController.navigate(EchoesRoute) {
                         popUpTo(DashboardRoute) { saveState = true }
                         launchSingleTop = true
                         restoreState = true
                    }
                }
                "REPLY" -> {
                   if (data.userId != null) {
                       navController.navigate(SendEchoRoute(userId = data.userId, username = data.username ?: "User", initialTab = 1))
                   }
                }
                "CONNECTION" -> {
                    navController.navigate(PeopleRoute) {
                        popUpTo(DashboardRoute) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }

            // Handle Direct Navigation Targets (overrides type-based if present)
            if (data.navigateTo == "JOURNEY") {
                 navController.navigate(CreditPremiumRoute(initialTab = 1)) {
                     launchSingleTop = true
                     restoreState = true
                 }
            }
        }
    }

    LaunchedEffect(deepLinkData) {
        // Handle initial deep link (e.g. from Android intent or cold start)
        if (deepLinkData != null) {
            com.bhanit.apps.echo.core.navigation.DeepLinkManager.handleDeepLink(deepLinkData)
        }
    }
    LaunchedEffect(Unit) {
        analytics.logEvent(com.bhanit.apps.echo.shared.analytics.EchoEvents.APP_OPEN)
        
        com.bhanit.apps.echo.core.auth.GlobalAuthEvents.logoutEvent.collect {
            println("DEBUG: App - Received Logout Event. Clearing session and navigating.")
            notificationHelper.cancelAll()
            sessionRepository.clearSession()
            navController.navigate(SplashRoute) {
                popUpTo(0) { inclusive = true }
            }
        }
    }
    
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { entry ->
            val route = entry.destination.route ?: "Unknown"
            analytics.logEvent(com.bhanit.apps.echo.shared.analytics.EchoEvents.SCREEN_VIEW, mapOf(
                com.bhanit.apps.echo.shared.analytics.EchoParams.SCREEN_NAME to route,
                com.bhanit.apps.echo.shared.analytics.EchoParams.SCREEN_CLASS to route
            ))
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                analytics.logEvent(com.bhanit.apps.echo.shared.analytics.EchoEvents.APP_BACKGROUND)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val currentTheme by sessionRepository.theme.collectAsState(ThemeConfig.SYSTEM)
    val customThemeColors by sessionRepository.customThemeColors.collectAsState(com.bhanit.apps.echo.core.theme.CustomThemeColors())
    val currentFont by sessionRepository.font.collectAsState(AppFont.INTER)
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    
    val appTheme = when (currentTheme) {
        ThemeConfig.SYSTEM -> if (isSystemDark) AppTheme.Dark else AppTheme.Light
        ThemeConfig.LIGHT -> AppTheme.Light
        ThemeConfig.DARK -> AppTheme.Dark
        ThemeConfig.GREEN_DARK -> AppTheme.GreenDark
        ThemeConfig.SOLAR -> AppTheme.Solar
        ThemeConfig.CUSTOM -> AppTheme.Custom
    }

    EchoTheme(appTheme = appTheme, appFont = currentFont, customThemeColors = customThemeColors) {
        org.koin.compose.KoinContext { // Setup KoinContext for Compose
            androidx.compose.material3.Surface(
                modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                color = androidx.compose.material3.MaterialTheme.colorScheme.background
            ) {
                CompositionLocalProvider(
                    LocalCoachMarkController provides coachMarkController,
                    LocalCoachMarkRepository provides coachMarkRepository
                ) {
                    CoachMarkOverlay {
                        NavHost(
                navController = navController,
            startDestination = SplashRoute,
            enterTransition = { slideInHorizontally { it } },
            exitTransition = { slideOutHorizontally { -it } },
            popEnterTransition = { slideInHorizontally { -it } },
            popExitTransition = { slideOutHorizontally { it } }
        ) {
            composable<SplashRoute> {
                SplashScreen(
                    onNavigateToLogin = {
                        navController.navigate(LoginRoute) {
                            popUpTo(SplashRoute) { inclusive = true }
                        }
                    },
                    onNavigateToDashboard = {
                        navController.navigate(DashboardRoute) {
                            popUpTo(SplashRoute) { inclusive = true }
                        }
                    },
                    onNavigateToOnboarding = {
                         navController.navigate(OnboardingRoute) {
                            popUpTo(SplashRoute) { inclusive = true }
                        }
                    },
                    onNavigateToWelcome = {
                        navController.navigate(WelcomeRoute) {
                            popUpTo(SplashRoute) { inclusive = true }
                        }
                    }
                )
            }



            composable<WelcomeRoute> {
                WelcomeScreen(
                    onNavigateToLogin = {
                        navController.navigate(LoginRoute) {
                            popUpTo(WelcomeRoute) { inclusive = true }
                        }
                    }
                )
            }

            composable<LoginRoute> {
                LoginScreen(
                    onNavigateToOtp = { phone ->
                        navController.navigate(OtpRoute(phone))
                    },
                    onNavigateToTerms = {
                        navController.navigate(PoliciesRoute(type = "TERMS"))
                    },
                    onNavigateToPrivacy = {
                        navController.navigate(PoliciesRoute(type = "PRIVACY"))
                    }
                )
            }

            composable<OtpRoute> { backStackEntry ->
                val route: OtpRoute = backStackEntry.toRoute()
                OtpScreen(
                    phone = route.phone,
                    onNavigateToOnboarding = {
                        navController.navigate(OnboardingRoute) {
                            popUpTo(LoginRoute) { inclusive = true }
                        }
                    },
                    onNavigateToDashboard = {
                        navController.navigate(DashboardRoute) {
                            popUpTo(LoginRoute) { inclusive = true }
                        }
                    }
                )
            }

            composable<OnboardingRoute> {
                OnboardingScreen(
                    onNavigateToDashboard = {
                        navController.navigate(DashboardRoute) {
                            popUpTo(OnboardingRoute) { inclusive = true }
                        }
                    },
                    onLogout = {
                        scope.launch {
                            sessionRepository.clearSession()
                            navController.navigate(SplashRoute) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable<DashboardRoute>(
                enterTransition = { fadeIn(animationSpec = tween(300)) },
                exitTransition = { fadeOut(animationSpec = tween(300)) },
                popEnterTransition = { fadeIn(animationSpec = tween(300)) },
                popExitTransition = { fadeOut(animationSpec = tween(300)) }
            ) {
                MainScaffold(navController) { padding ->
                    Box(modifier = androidx.compose.ui.Modifier.padding(bottom = padding.calculateBottomPadding())) {
                        com.bhanit.apps.echo.features.dashboard.presentation.DashboardScreen(
                            onNavigateToSend = { navController.navigate(PeopleRoute) },
                            onNavigateToInbox = { navController.navigate(EchoesRoute) },
                            onNavigateToSettings = { navController.navigate(SettingsRoute) },
                            onNavigateToProfile = { navController.navigate(ProfileRoute) },
                            onNavigateToScanQr = { navController.navigate(ScanQrRoute) },
                            onNavigateToStreaks = { navController.navigate(StreaksRoute) },
                            onNavigateToJourney = { navController.navigate(CreditPremiumRoute(initialTab = 1)) }
                        )
                    }
                }
            }

            composable<EchoesRoute>(
                enterTransition = { fadeIn(animationSpec = tween(300)) },
                exitTransition = { fadeOut(animationSpec = tween(300)) },
                popEnterTransition = { fadeIn(animationSpec = tween(300)) },
                popExitTransition = { fadeOut(animationSpec = tween(300)) }
            ) {
                MainScaffold(navController) { padding ->
                    Box(modifier = androidx.compose.ui.Modifier.padding(bottom = padding.calculateBottomPadding())) {
                        com.bhanit.apps.echo.features.messaging.presentation.EchoesScreen(
                            onNavigateBack = { /* Top level */ }
                        )
                    }
                }
            }

            composable<PeopleRoute>(
                enterTransition = { fadeIn(animationSpec = tween(300)) },
                exitTransition = { fadeOut(animationSpec = tween(300)) },
                popEnterTransition = { fadeIn(animationSpec = tween(300)) },
                popExitTransition = { fadeOut(animationSpec = tween(300)) }
            ) {
                MainScaffold(navController) { padding ->
                    Box(modifier = androidx.compose.ui.Modifier.padding(bottom = padding.calculateBottomPadding())) {
                        PeopleScreen(
                            onNavigateBack = { /* Top level */ },
                            onNavigateToChat = { userId, username ->
                                navController.navigate(SendEchoRoute(userId, username))
                            },
                            onNavigateToFindFriends = {
                                navController.navigate(AvailableContactsRoute)
                            },
                            onNavigateToProfile = { userId ->
                                navController.navigate(ProfileRoute) // Assuming ProfileRoute takes userId or generic?
                            }
                        )
                    }
                }
            }
            
            composable<ScanQrRoute> {
                com.bhanit.apps.echo.features.contact.presentation.scan.ScanQrScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToChat = { userId, username ->
                        navController.navigate(SendEchoRoute(userId, username)) {
                             popUpTo(ScanQrRoute) { inclusive = true }
                        }
                    }
                )
            }



            composable<SettingsRoute>(
                enterTransition = { fadeIn(animationSpec = tween(300)) },
                exitTransition = { fadeOut(animationSpec = tween(300)) },
                popEnterTransition = { fadeIn(animationSpec = tween(300)) },
                popExitTransition = { fadeOut(animationSpec = tween(300)) }
            ) {
                MainScaffold(navController) { padding ->
                    Box(modifier = androidx.compose.ui.Modifier.padding(bottom = padding.calculateBottomPadding())) {
                        com.bhanit.apps.echo.features.settings.presentation.settings.SettingsScreen(
                            onNavigateToProfile = { navController.navigate(ProfileRoute) },
                            onNavigateToBlocked = { navController.navigate(BlockedListRoute) },
                            onNavigateToTerms = { navController.navigate(PoliciesRoute(type = "TERMS")) },
                            onNavigateToPrivacy = { navController.navigate(PoliciesRoute(type = "PRIVACY")) },
                            onNavigateToAbout = { navController.navigate(AboutRoute) },
                            onNavigateToRefer = { navController.navigate(ReferralRoute) },
                            onNavigateToPremium = { navController.navigate(CreditPremiumRoute(initialTab = 0)) },
                            onNavigateToSupport = { navController.navigate(com.bhanit.apps.echo.core.navigation.SupportRoute) },
                            onNavigateToTheme = { navController.navigate(com.bhanit.apps.echo.core.navigation.ThemeRoute) },
                            onNavigateToNotifications = {
                                navController.navigate(
                                    NotificationSettingsRoute
                                )
                            },
                            onLogout = {
                                notificationHelper.cancelAll()
                                scope.launch {
                                    sessionRepository.clearSession()
                                    navController.navigate(SplashRoute) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            },
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToStreaks = { navController.navigate(StreaksRoute) }
                        )
                    }
                }
            }

            composable<PoliciesRoute> { backStackEntry ->
                val route: PoliciesRoute = backStackEntry.toRoute()
                PoliciesScreen(
                    type = route.type,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            // AppSettingsRoute removed/unused

            composable<AboutRoute> {
                AboutScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Nested routes accessible from anywhere
            composable<SendEchoRoute> { backStackEntry ->
                val route: SendEchoRoute = backStackEntry.toRoute()
                SendEchoScreen(
                    userId = route.userId,
                    username = route.username,
                    initialTab = route.initialTab,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable<ProfileRoute> {
                ProfileScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable<ReferralRoute> {
                com.bhanit.apps.echo.features.credits.presentation.ReferralScreen(
                     onBack = { navController.popBackStack() }
                )
            }

            composable<CreditPremiumRoute> { backStackEntry ->
                val route: CreditPremiumRoute = backStackEntry.toRoute()
                com.bhanit.apps.echo.features.credits.presentation.CreditPremiumScreen(
                    initialTab = route.initialTab,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<BlockedListRoute> {
                 com.bhanit.apps.echo.features.settings.presentation.blocked.BlockedListScreen(
                     onNavigateBack = { navController.popBackStack() }
                 )
            }
            composable<NotificationSettingsRoute> {
                NotificationSettingsScreen (
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable<com.bhanit.apps.echo.core.navigation.ThemeRoute> {
                com.bhanit.apps.echo.features.settings.presentation.theme.ThemeScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<com.bhanit.apps.echo.core.navigation.SupportRoute> {
                com.bhanit.apps.echo.features.settings.presentation.support.SupportScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCreateTicket = { 
                        navController.navigate(com.bhanit.apps.echo.core.navigation.CreateSupportRoute) 
                    }
                )
            }

            composable<com.bhanit.apps.echo.core.navigation.CreateSupportRoute> {
                com.bhanit.apps.echo.features.settings.presentation.support.CreateSupportScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<AvailableContactsRoute> {
                com.bhanit.apps.echo.features.contact.presentation.available.AvailableContactsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<ContactSyncRoute> {
                ContactSyncScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            composable<StreaksRoute> {
                com.bhanit.apps.echo.features.settings.presentation.streaks.StreaksScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            }
        }
                    }
                }
        
        // Update Overlay
        if (updateState.status != com.bhanit.apps.echo.core.config.UpdateStatus.NONE) {
            com.bhanit.apps.echo.core.presentation.components.UpdateOverlay(
                status = updateState.status,
                updateUrl = updateState.updateUrl,
                onUpdateClick = { url ->
                    val finalUrl = url.ifBlank {
                        // Fallback logic if URL is empty in config
                        if (getPlatform().name.contains("Android"))
                            "https://play.google.com/store/apps/details?id=com.bhanit.apps.echo"
                        else
                            "https://apps.apple.com/in/app/echo-feelings-come-back/id6757484280?ct=echo_in_app_update"
                    }
                    platformUtils.openUrl(finalUrl)
                },
                onDismissOptional = {
                    updateManager.dismissUpdate()
                }
            )
            }
        }
    }
}
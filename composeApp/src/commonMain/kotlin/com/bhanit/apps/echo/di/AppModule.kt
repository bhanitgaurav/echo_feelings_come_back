package com.bhanit.apps.echo.di

import MediaApi
import com.bhanit.apps.echo.core.network.HttpClientFactory
import com.bhanit.apps.echo.features.auth.data.AuthApi
import com.bhanit.apps.echo.features.auth.data.AuthRepositoryImpl
import com.bhanit.apps.echo.features.auth.domain.AuthRepository
import com.bhanit.apps.echo.features.auth.domain.SessionRepository
import com.bhanit.apps.echo.features.auth.presentation.login.LoginViewModel
import com.bhanit.apps.echo.features.auth.presentation.otp.OtpViewModel
import com.bhanit.apps.echo.features.contact.data.ContactRepositoryImpl
import com.bhanit.apps.echo.features.contact.domain.ContactRepository
import com.bhanit.apps.echo.features.contact.presentation.available.AvailableContactsViewModel
import com.bhanit.apps.echo.features.contact.presentation.list.PeopleViewModel
import com.bhanit.apps.echo.features.contact.presentation.sync.ContactSyncViewModel
import com.bhanit.apps.echo.features.contact.presentation.scan.ScanQrViewModel
import com.bhanit.apps.echo.features.credits.presentation.CreditsViewModel
import com.bhanit.apps.echo.features.dashboard.presentation.DashboardViewModel
import com.bhanit.apps.echo.features.messaging.data.EmotionRepositoryImpl
import com.bhanit.apps.echo.features.messaging.data.MessagingRepositoryImpl
import com.bhanit.apps.echo.features.messaging.domain.EmotionRepository
import com.bhanit.apps.echo.features.messaging.domain.MessagingRepository
import com.bhanit.apps.echo.features.messaging.presentation.EchoesViewModel
import com.bhanit.apps.echo.features.messaging.presentation.SendEchoViewModel
import com.bhanit.apps.echo.features.onboarding.OnboardingViewModel
import com.bhanit.apps.echo.features.settings.data.SupportRepositoryImpl
import com.bhanit.apps.echo.features.settings.presentation.blocked.BlockedListViewModel
import com.bhanit.apps.echo.features.settings.presentation.profile.ProfileViewModel
import com.bhanit.apps.echo.features.settings.presentation.settings.SettingsViewModel
import com.bhanit.apps.echo.features.settings.presentation.support.SupportViewModel
import com.bhanit.apps.echo.features.settings.presentation.theme.ThemeViewModel
import com.bhanit.apps.echo.features.splash.SplashViewModel
import com.bhanit.apps.echo.features.user.data.UserApi
import com.bhanit.apps.echo.features.user.data.UserRepositoryImpl
import com.bhanit.apps.echo.features.user.domain.UserRepository
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import com.bhanit.apps.echo.core.tutorial.CoachMarkRepository
import com.bhanit.apps.echo.core.tutorial.CoachMarkRepositoryImpl
import com.bhanit.apps.echo.data.repository.CloudinaryRepositoryImpl
import com.bhanit.apps.echo.features.settings.presentation.about.AboutViewModel
import com.bhanit.apps.echo.features.settings.presentation.settings.NotificationSettingsViewModel
import com.bhanit.apps.echo.features.settings.presentation.settings.PoliciesViewModel
import com.bhanit.apps.echo.features.welcome.WelcomeViewModel

import org.koin.dsl.module

val appModule = module {
    includes(platformModule)
    // HTTP Client (Injected with dynamic Base URL from AppConfig)
    single { 
        val config: com.bhanit.apps.echo.core.config.AppConfig = get()
        HttpClientFactory(get(), config.baseUrl).create() 
    }
    single { 
        val config: com.bhanit.apps.echo.core.config.AppConfig = get()
        com.bhanit.apps.echo.features.dashboard.data.DashboardApi(get(), config.baseUrl) 
    }
    single { 
        val config: com.bhanit.apps.echo.core.config.AppConfig = get()
        com.bhanit.apps.echo.features.habits.data.HabitApi(get(), config.baseUrl) 
    }
    singleOf(::MediaApi)
    singleOf(::CloudinaryRepositoryImpl)
    single { com.bhanit.apps.echo.core.config.UpdateManager(get(), get()) }

    
    singleOf(::SessionRepository)
    singleOf(::CoachMarkRepositoryImpl) bind CoachMarkRepository::class
    
    single { 
        val config: com.bhanit.apps.echo.core.config.AppConfig = get()
        com.bhanit.apps.echo.features.auth.data.AuthApi(get(), config.baseUrl) 
    }
    singleOf(::AuthRepositoryImpl) bind AuthRepository::class
    factory { com.bhanit.apps.echo.features.contact.manager.createContactManager() }
    single { 
        val config: com.bhanit.apps.echo.core.config.AppConfig = get()
        ContactRepositoryImpl(get(), get(), get(), get<com.bhanit.apps.echo.core.util.ShareManager>(), config.baseUrl) 
    } bind ContactRepository::class
    single {
        val config: com.bhanit.apps.echo.core.config.AppConfig = get()
        EmotionRepositoryImpl(get(), config.baseUrl)
    } bind EmotionRepository::class
    single { 
        val config: com.bhanit.apps.echo.core.config.AppConfig = get()
        MessagingRepositoryImpl(get(), get(), config.baseUrl) 
    } bind MessagingRepository::class

    single { 
        val config: com.bhanit.apps.echo.core.config.AppConfig = get()
        UserApi(get(), config.baseUrl) 
    }
    singleOf(::UserRepositoryImpl) bind UserRepository::class
    
    // Updated Support Repo
    single {
         val config: com.bhanit.apps.echo.core.config.AppConfig = get()
         SupportRepositoryImpl(get(), config.baseUrl)
    } bind com.bhanit.apps.echo.features.settings.domain.SupportRepository::class
    
    viewModelOf(::LoginViewModel)
    viewModelOf(::OtpViewModel)
    viewModelOf(::SplashViewModel)
    viewModelOf(::WelcomeViewModel)
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::DashboardViewModel)
    viewModelOf(::PeopleViewModel)

    viewModelOf(::SendEchoViewModel)
    viewModelOf(::EchoesViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::NotificationSettingsViewModel)
    viewModelOf(::ThemeViewModel) // Added
    viewModelOf(::ProfileViewModel)
    viewModelOf(::ContactSyncViewModel)
    viewModelOf(::BlockedListViewModel)
    viewModelOf(::AvailableContactsViewModel)
    viewModelOf(::CreditsViewModel)
    viewModelOf(::ScanQrViewModel)

    // Content API
    single { 
        val config: com.bhanit.apps.echo.core.config.AppConfig = get()
        com.bhanit.apps.echo.features.settings.data.ContentApi(
            client = get(), 
            baseUrl = config.baseUrl
        ) 
    }
    single { com.bhanit.apps.echo.features.settings.domain.ContentRepository(get()) }
    viewModelOf(::AboutViewModel)
    viewModelOf(::PoliciesViewModel)

    
    // singleOf(::SupportRepositoryImpl) bind com.bhanit.apps.echo.features.settings.domain.SupportRepository::class // Moved above
    viewModelOf(::SupportViewModel)

}

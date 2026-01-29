package com.bhanit.apps.echo.features.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bhanit.apps.echo.core.designsystem.components.EchoButton
import com.bhanit.apps.echo.core.theme.BackgroundLight
import com.bhanit.apps.echo.core.theme.PrimaryEcho
import org.koin.compose.viewmodel.koinViewModel

import echo.composeapp.generated.resources.Res
import echo.composeapp.generated.resources.echo_logo_text
import org.jetbrains.compose.resources.painterResource

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToWelcome: () -> Unit
) {
    val viewModel = koinViewModel<SplashViewModel>()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.navigateTo) {
        when (state.navigateTo) {
            SplashDestination.LOGIN -> onNavigateToLogin()
            SplashDestination.DASHBOARD -> onNavigateToDashboard()
            SplashDestination.ONBOARDING -> onNavigateToOnboarding()
            SplashDestination.WELCOME -> onNavigateToWelcome()
            null -> Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {


            // Brand Logo
            androidx.compose.foundation.Image(
                painter = painterResource(Res.drawable.echo_logo_text),
                contentDescription = "ECHO Logo",
                modifier = Modifier.width(240.dp) // Wider for text logo
            )
            // Text removed as it is part of the logo image

            Spacer(modifier = Modifier.height(64.dp))
            
            if (state.needsBiometric) {
                EchoButton(
                    text = "Unlock with Biometrics",
                    onClick = { viewModel.doBiometricAuth() },
                    icon = androidx.compose.material.icons.Icons.Default.Fingerprint,
                    modifier = Modifier.padding(horizontal = 48.dp)
                )
            } else {
                CircularProgressIndicator(
                    color = PrimaryEcho,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

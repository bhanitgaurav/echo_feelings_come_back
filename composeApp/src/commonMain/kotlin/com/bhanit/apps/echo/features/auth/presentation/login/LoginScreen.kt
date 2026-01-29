package com.bhanit.apps.echo.features.auth.presentation.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.bhanit.apps.echo.core.designsystem.components.EchoButton
import com.bhanit.apps.echo.core.designsystem.components.EchoCard
import com.bhanit.apps.echo.core.presentation.components.DisableInputActions
import echo.composeapp.generated.resources.Res
import echo.composeapp.generated.resources.echo_logo
import echo.composeapp.generated.resources.echo_logo_text
import echo.composeapp.generated.resources.ic_whatsapp
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginScreen(
    onNavigateToOtp: (String) -> Unit,
    onNavigateToTerms: () -> Unit,
    onNavigateToPrivacy: () -> Unit
) {
    val viewModel = koinViewModel<LoginViewModel>()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isOtpSent) {
        if (state.isOtpSent) {
            onNavigateToOtp(state.sentPhone ?: state.phone)
            viewModel.onOtpSentHandled()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        DisableInputActions {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(1f))



                // Logo
                androidx.compose.foundation.Image(
                    painter = painterResource(Res.drawable.echo_logo_text),
                    contentDescription = "ECHO Logo",
                    modifier = Modifier.width(200.dp)
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Glass Container for Form
                EchoCard(
                    // backgroundColor = SurfaceGlass, // Use default
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Phone Input Row
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
                                8.dp
                            )
                        ) {
                            // Country Code Picker (Simplified)
                            var showCountryPicker by remember {
                                androidx.compose.runtime.mutableStateOf(
                                    false
                                )
                            }


                            OutlinedTextField(
                                value = state.countryCode,
                                onValueChange = { }, // Read-only via picker
                                modifier = Modifier
                                    .width(80.dp)
                                    .clickable { showCountryPicker = true },
                                enabled = true, // We want it to look enabled (dark text)
                                readOnly = true, // But preventing typing
                                label = { Text("Code") }, // Match height with Phone field
                                textStyle = MaterialTheme.typography.bodyLarge,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary, // Show active color if focused
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(
                                        alpha = 0.3f
                                    ),
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )

                            if (showCountryPicker) {
                                CountryCodePickerDialog(
                                    onDismiss = { showCountryPicker = false },
                                    onCountrySelected = { code ->
                                        viewModel.onCountryCodeChange(code)
                                        showCountryPicker = false
                                    }
                                )
                            }

                            // Phone Number
                            OutlinedTextField(
                                value = state.phone,
                                onValueChange = viewModel::onPhoneChange,
                                label = { Text("Phone Number") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.weight(1f),
                                isError = state.error != null,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(
                                        alpha = 0.3f
                                    ),
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )
                        }

                        if (state.error != null) {
                            Text(
                                text = state.error ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 8.dp).align(Alignment.Start)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        EchoButton(
                            text = if (state.isLoading) "Sending..." else "Send Code",
                            onClick = viewModel::sendOtp,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = state.isContinueEnabled && !state.isLoading,
                            icon = org.jetbrains.compose.resources.vectorResource(Res.drawable.ic_whatsapp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Footer
                val annotatedString = androidx.compose.ui.text.buildAnnotatedString {
                    append("By continuing, you agree to our ")

                    pushStringAnnotation(tag = "terms", annotation = "terms")
                    withStyle(
                        style = androidx.compose.ui.text.SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append("Terms & Conditions")
                    }
                    pop()

                    append(" and ")

                    pushStringAnnotation(
                        tag = "privacy",
                        annotation = "privacy"
                    )
                    withStyle(
                        style = androidx.compose.ui.text.SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append("Privacy Policy")
                    }
                    pop()

                    append(".")
                }

                androidx.compose.foundation.text.ClickableText(
                    text = annotatedString,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(tag = "terms", start = offset, end = offset)
                            .firstOrNull()?.let {
                                onNavigateToTerms()
                            }
                        
                        annotatedString.getStringAnnotations(tag = "privacy", start = offset, end = offset)
                            .firstOrNull()?.let {
                                onNavigateToPrivacy()
                            }
                    }
                )
            }
        }
    }
}

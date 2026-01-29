package com.bhanit.apps.echo.features.auth.presentation.login

import com.bhanit.apps.echo.core.base.BaseViewModel
import com.bhanit.apps.echo.features.auth.domain.AuthRepository
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope

data class LoginState(
    val isLoading: Boolean = false,
    val phone: String = "",
    val countryCode: String = "+91",
    val error: String? = null,
    val isVerified: Boolean = false,
    val isOtpSent: Boolean = false,
    val sentPhone: String? = null,
    val isContinueEnabled: Boolean = false
)

class LoginViewModel(
    private val authRepository: AuthRepository
) : BaseViewModel<LoginState>(LoginState()) {

    // Regex: Starts with 6-9, followed by 9 digits (total 10)
    private val phoneRegex = Regex("^[6-9][0-9]{9}$")

    fun onPhoneChange(phone: String) {
        // 1. Strict Numeric Filter
        val numericPhone = phone.filter { it.isDigit() }
        
        // 2. Restrict to max 10 digits
        val constrainedPhone = if (numericPhone.length > 10) numericPhone.take(10) else numericPhone
        
        // 3. Immediate Validation on First Digit
        var newError: String? = null
        if (constrainedPhone.isNotEmpty()) {
            val firstChar = constrainedPhone.first()
            if (firstChar !in '6'..'9') {
                newError = "Invalid number"
            }
        }

        // 3. Check Complete Validity for Button
        val isValid = phoneRegex.matches(constrainedPhone)
        
        updateState { 
            it.copy(
                phone = constrainedPhone, 
                error = newError,
                isContinueEnabled = isValid
            ) 
        }
    }

    fun onCountryCodeChange(code: String) {
        updateState { it.copy(countryCode = code, error = null) }
    }

    fun sendOtp() {
        val rawPhone = state.value.phone
        val countryCode = state.value.countryCode
        
        if (!state.value.isContinueEnabled) {
            return
        }
        
        // Normalize phone number using the selected country code
        val normalizedPhone = com.bhanit.apps.echo.core.util.PhoneNumberUtils.normalizePhoneNumber(rawPhone, countryCode)
        
        updateState { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            authRepository.sendOtp(normalizedPhone)
                .onSuccess {
                    // Do NOT overwrite 'phone' with normalizedPhone to avoid UI issues when navigating back
                    updateState { it.copy(isLoading = false, isOtpSent = true, sentPhone = normalizedPhone) } 
                }
                .onFailure { error ->
                    updateState { it.copy(isLoading = false, error = error.message ?: "Failed to send OTP") }
                }
        }
    }
    
    fun onOtpSentHandled() {
        updateState { it.copy(isOtpSent = false, sentPhone = null) }
    }
}

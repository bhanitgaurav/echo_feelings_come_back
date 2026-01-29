package com.bhanit.apps.echo.features.onboarding

import com.bhanit.apps.echo.core.base.BaseViewModel
import com.bhanit.apps.echo.features.auth.domain.SessionRepository
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope

import com.bhanit.apps.echo.features.user.domain.UserRepository

import com.bhanit.apps.echo.data.repository.CloudinaryRepositoryImpl
import com.bhanit.apps.echo.shared.domain.model.Inspiration

data class OnboardingState(
    val isLoading: Boolean = false,
    val isCompleted: Boolean = false,
    val username: String = "",
    val referralCode: String = "",
    val error: String? = null,
    val isPhotoUploading: Boolean = false,
    val photoUrl: String? = null,
    val photoUploadError: String? = null,
    val isGeneratingUsername: Boolean = false
)

class OnboardingViewModel(
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository,
    private val cloudinaryRepository: CloudinaryRepositoryImpl,
    private val imageCompressor: com.bhanit.apps.echo.core.utils.ImageCompressor,
    private val analytics: com.bhanit.apps.echo.shared.analytics.EchoAnalytics,
    private val platformUtils: com.bhanit.apps.echo.core.util.PlatformUtils
) : BaseViewModel<OnboardingState>(OnboardingState()) {

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            // Listen for referral code from deep links
            launch {
                com.bhanit.apps.echo.core.navigation.DeepLinkManager.deepLinkEvent.collect { event ->
                    if (!event.referralCode.isNullOrBlank()) {
                        updateState { it.copy(referralCode = event.referralCode) }
                    }
                }
            }
            
            // Check clipboard for referral code (fallback for iOS/Manual copy)
            launch {
                checkClipboardForReferral()
            }
            
            // Check for saved Install Referrer (Android Play Store)
            launch {
                sessionRepository.referralCode.collect { code ->
                    if (!code.isNullOrBlank() && state.value.referralCode.isEmpty()) {
                         updateState { it.copy(referralCode = code) }
                    }
                }
            }

            userRepository.getProfile()
                .onSuccess { profile ->
                    updateState { 
                        it.copy(
                            username = profile.username ?: "",
                            photoUrl = profile.photoUrl,
                            // Ensure referral code is not overwritten if user entered it (though unlikely flow)
                        ) 
                    }
                }
                .onFailure { 
                    // Silent failure or log? For onboarding, we just want to pre-fill if available.
                    // If 404/new user, result might vary, but typically getProfile returns current user.
                }
        }
    }

    private suspend fun checkClipboardForReferral() {
        val clipboardText = platformUtils.getClipboardText()
        if (!clipboardText.isNullOrBlank()) {
            // Assuming referral code format is alphanumeric, length 6-12 (adjust regex as needed)
            // Example: ECHO123 or just 123456
            // We want to avoid pasting entire sentences.
            val referralRegex = Regex("^[A-Z0-9]{6,12}$", RegexOption.IGNORE_CASE)
            
            if (referralRegex.matches(clipboardText.trim())) {
                // Only autofill if current state is empty
                if (state.value.referralCode.isEmpty()) {
                    updateState { it.copy(referralCode = clipboardText.trim()) }
                }
            }
        }
    }

    fun onUsernameChange(value: String) {
        updateState { it.copy(username = value, error = null) }
    }

    fun onReferralCodeChange(value: String) {
        updateState { it.copy(referralCode = value, error = null) }
    }

    fun completeOnboarding() {
        val currentState = state.value
        if (currentState.username.length < 8 || currentState.username.length > 20) {
            updateState { it.copy(error = "Username must be between 8 and 20 characters") }
            return
        }

        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null) }
            
            val result = userRepository.completeOnboarding(
                username = currentState.username,
                referralCode = currentState.referralCode.ifBlank { null },
                photoUrl = currentState.photoUrl
            )

            if (result.isSuccess) {
                sessionRepository.setOnboardingCompleted(true)
                analytics.logEvent(com.bhanit.apps.echo.shared.analytics.EchoEvents.ONBOARDING_COMPLETED)
                updateState { it.copy(isLoading = false, isCompleted = true) }
            } else {
                updateState { it.copy(isLoading = false, error = result.exceptionOrNull()?.message ?: "Onboarding Failed") }
            }
        }

    }

    fun uploadPhoto(bytes: ByteArray) {
        val maxSizeBytes = 10 * 1024 * 1024
        if (bytes.size > maxSizeBytes) {
            updateState { it.copy(error = "Image size exceeds 10MB limit") }
            return
        }

        updateState { it.copy(isPhotoUploading = true, photoUploadError = null) }
        viewModelScope.launch {
            val compressedBytes = imageCompressor.compress(bytes)
            
            cloudinaryRepository.uploadImage(compressedBytes)
                .onSuccess { url ->
                     // Append random version to bypass cache
                     val cacheBustedUrl = "$url?v=${kotlin.random.Random.nextInt()}"
                     
                     // Persist to server immediately
                     val currentState = state.value
                     // We need current profile to not strictly overwrite other fields with empty if they exist?
                     // UpdateProfileRequest usually updates only what is sent? 
                     // But typical REST PUT replaces resource or PATCH updates partial.
                     // The plan says: Fetch, then Update.
                     
                     userRepository.getProfile().onSuccess { profile ->
                         val request = com.bhanit.apps.echo.data.model.UpdateProfileRequest(
                             fullName = profile.fullName ?: "",
                             username = currentState.username.ifBlank { profile.username ?: "" }, // Use current input or existing
                             gender = profile.gender ?: "",
                             email = profile.email ?: "",
                             photoUrl = cacheBustedUrl
                         )
                         
                         userRepository.updateProfile(request)
                            .onSuccess {
                                updateState { it.copy(isPhotoUploading = false, photoUrl = cacheBustedUrl) }
                            }
                            .onFailure { error ->
                                updateState { it.copy(isPhotoUploading = false, photoUploadError = "Failed to save to profile: ${error.message}") }
                            }
                     }.onFailure { error ->
                         // If getProfile fails, we can't safely update? Or we assume new user?
                         // If getProfile fails, maybe just update state and hope completeOnboarding saves it later?
                         // But the user issue is "logout and login... persistence". 
                         // So we MUST save it.
                         // Fallback: Try to update with just photo if possible or assume defaults.
                         // For now, fail the upload flow if we can't sync.
                         updateState { it.copy(isPhotoUploading = false, photoUploadError = "Failed to sync profile: ${error.message}") }
                     }
                }
                .onFailure { error ->
                    updateState { it.copy(isPhotoUploading = false, photoUploadError = error.message ?: "Upload failed") }
                }
        }
    }


    fun generateUsername(inspiration: Inspiration?) {
         updateState { it.copy(isGeneratingUsername = true, error = null) }
         viewModelScope.launch {
             userRepository.generateUsername(inspiration)
                 .onSuccess { newUsername ->
                     updateState { it.copy(isGeneratingUsername = false, username = newUsername) }
                 }
                 .onFailure { error ->
                     updateState { 
                         it.copy(
                             isGeneratingUsername = false, 
                             error = "Failed to generate username"
                         ) 
                     }
                 }
         }
    }
}

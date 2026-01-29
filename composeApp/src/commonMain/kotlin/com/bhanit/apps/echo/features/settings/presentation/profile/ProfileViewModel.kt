package com.bhanit.apps.echo.features.settings.presentation.profile

import com.bhanit.apps.echo.core.base.BaseViewModel
import com.bhanit.apps.echo.data.model.UpdateProfileRequest
import com.bhanit.apps.echo.features.user.domain.UserRepository
import kotlinx.coroutines.launch
import com.bhanit.apps.echo.core.network.AppException
import androidx.lifecycle.viewModelScope
import com.bhanit.apps.echo.data.repository.CloudinaryRepositoryImpl
import com.bhanit.apps.echo.shared.domain.model.Inspiration

data class ProfileState(
    val isLoading: Boolean = true,
    val fullName: String = "",
    val username: String = "",
    val gender: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val photoUrl: String = "",
    val referralUrl: String = "",

    val error: AppException? = null, // Load error
    val isSaved: Boolean = false,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val validationError: String? = null,
    val isPhotoUploading: Boolean = false,
    val photoUploadError: String? = null,
    val snackbarMessage: String? = null,
    val isGeneratingUsername: Boolean = false
)




class ProfileViewModel(
    private val userRepository: UserRepository,
    private val cloudinaryRepository: CloudinaryRepositoryImpl,
    private val imageCompressor: com.bhanit.apps.echo.core.utils.ImageCompressor,
    private val sessionRepository: com.bhanit.apps.echo.features.auth.domain.SessionRepository
) : BaseViewModel<ProfileState>(ProfileState()) {

    private fun loadProfile() {
        updateState { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            userRepository.getProfile()
                .onSuccess { profile ->
                    updateState { 
                        it.copy(
                            isLoading = false,
                            fullName = profile.fullName ?: "",
                            username = profile.username ?: "",
                            gender = profile.gender ?: "",
                            email = profile.email ?: "",
                            phoneNumber = profile.phoneNumber,
                            photoUrl = profile.photoUrl ?: "",
                            referralUrl = profile.referralUrl ?: ""
                        )
                    }
                }
                .onFailure { error ->
                    updateState { it.copy(isLoading = false, error = error as? AppException) }
                }
        }
    }
    
    fun reload() {
        loadProfile()
    }

    fun onFullNameChange(value: String) {
        updateState { it.copy(fullName = value) }
    }

    fun onUsernameChange(value: String) {
        updateState { it.copy(username = value) }
    }

    fun onGenderChange(value: String) {
        updateState { it.copy(gender = value) }
    }

    fun onEmailChange(value: String) {
        updateState { it.copy(email = value) }
    }

    fun saveProfile() {
        val state = state.value
        val trimmedFullName = state.fullName.trim()
        val trimmedUsername = state.username.trim()
        val trimmedEmail = state.email.trim()

        if (trimmedFullName.length > 50) {
            updateState { it.copy(validationError = "Full name cannot exceed 50 characters.") }
            return
        }
        
        if (trimmedFullName.isNotEmpty() && !trimmedFullName.matches(Regex("^[a-zA-Z ]+$"))) {
             updateState { it.copy(validationError = "Full name can only contain letters and spaces.") }
             return
        }

        if (trimmedUsername.length < 8 || trimmedUsername.length > 20) {
             updateState { it.copy(validationError = "Username must be between 8 and 20 characters.") }
             return
        }

        if (!trimmedUsername.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            updateState { it.copy(validationError = "Username can only contain letters, numbers, and underscores.") }
            return
        }

        if (trimmedEmail.isNotEmpty() && !trimmedEmail.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"))) {
            updateState { it.copy(validationError = "Invalid email format.") }
            return
        }

        updateState { it.copy(isSaving = true, saveError = null, validationError = null, isSaved = false) }
        viewModelScope.launch {
            val request = UpdateProfileRequest(
                fullName = trimmedFullName,
                username = trimmedUsername,
                gender = state.gender,
                email = trimmedEmail,
                photoUrl = state.photoUrl
            )
            userRepository.updateProfile(request)
                .onSuccess {
                    sessionRepository.updateSessionUserInfo(username = trimmedUsername)
                    updateState { it.copy(isSaving = false, isSaved = true) }
                }
                .onFailure { error ->
                    updateState { it.copy(isSaving = false, saveError = error.message) }
                }
        }
    }

    // Expose a side effect for Snackbar, or use a state property that the UI observes and resets
    // For simplicity, using a state property 'snackbarMessage' in ProfileState would be better.
    // However, since ProfileState doesn't have it, I'll add it using copy on the state.
    // Use 'photoUploadError' for now or add a new field if needed. The requirement says "show snackbar".
    // I will use validationError for transient UI messages if appropriate, or better, reuse photoUploadError
    // but the UI typically shows that under the image.
    // Let's use photoUploadError for size issues as well, but the prompt says snackbar.
    // I'll add a specific error state for snackbar if I can modify ProfileState.
    // Since I can replace the whole file content or large chunk, I'll add `snackbarMessage` to ProfileState.

    fun uploadPhoto(bytes: ByteArray) {
        // 10MB limit (10 * 1024 * 1024)
        val maxSizeBytes = 10 * 1024 * 1024
        if (bytes.size > maxSizeBytes) {
            updateState { it.copy(snackbarMessage = "Image size exceeds 10MB limit") }
            return
        }

        updateState { it.copy(isPhotoUploading = true, photoUploadError = null, snackbarMessage = null) }
        viewModelScope.launch {
            // Compress
            val compressedBytes = imageCompressor.compress(bytes)

            cloudinaryRepository.uploadImage(compressedBytes)
                .onSuccess { url ->
                    // Append random version to bypass cache
                    val cacheBustedUrl = "$url?v=${kotlin.random.Random.nextInt()}"
                    updateState { it.copy(isPhotoUploading = false, photoUrl = cacheBustedUrl) }
                    
                    val currentState = state.value
                    val request = UpdateProfileRequest(
                        fullName = currentState.fullName,
                        username = currentState.username,
                        gender = currentState.gender,
                        email = currentState.email,
                        photoUrl = cacheBustedUrl
                    )
                    // Update profile on server without triggering 'isSaved' navigation
                    userRepository.updateProfile(request)
                        .onSuccess {
                            // Valid: Photo saved. Stay on screen.
                            sessionRepository.updateSessionUserInfo(photoUrl = cacheBustedUrl)
                        }
                        .onFailure { error ->
                            // If saving the URL fails, show error (or maybe just log it/show toast)
                             updateState { it.copy(photoUploadError = "Photo uploaded but failed to save to profile: ${error.message}") }
                        }
                }
                .onFailure { error ->
                    updateState { it.copy(isPhotoUploading = false, photoUploadError = error.message ?: "Upload failed") }
                }
        }
    }

    fun clearSnackbar() {
        updateState { it.copy(snackbarMessage = null) }
    }

    fun generateUsername(inspiration: Inspiration?) {
        updateState { it.copy(isGeneratingUsername = true, snackbarMessage = null) }
        viewModelScope.launch {
            userRepository.generateUsername(inspiration)
                .onSuccess { newUsername ->
                    updateState { it.copy(isGeneratingUsername = false, username = newUsername) }
                }
                .onFailure { error ->
                    updateState { 
                        it.copy(
                            isGeneratingUsername = false, 
                            snackbarMessage = "Failed to generate username"
                        ) 
                    }
                }
        }
    }
}


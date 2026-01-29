package com.bhanit.apps.echo.features.contact.presentation.scan

import androidx.lifecycle.viewModelScope
import com.bhanit.apps.echo.core.base.BaseViewModel
import com.bhanit.apps.echo.features.contact.domain.ContactRepository
import kotlinx.coroutines.launch

data class ScanQrState(
    val isScanning: Boolean = true,
    val isLoading: Boolean = false,
    val result: com.bhanit.apps.echo.data.model.ReferralResponse? = null,
    val error: String? = null
)

class ScanQrViewModel(
    private val contactRepository: ContactRepository,
    private val userRepository: com.bhanit.apps.echo.features.user.domain.UserRepository
) : BaseViewModel<ScanQrState>(ScanQrState()) {

    fun onCodeScanned(urlOrCode: String) {
        if (!state.value.isScanning || state.value.isLoading) return

        // Validate logic
        // URL Format: https://echo.app/r?code=XYZ or similar. Or just XYZ?
        // Let's assume URL or just code.
        // If URL, extract 'code' param or last path segment if formatted like /r/XYZ
        
        val code = extractCodeFromUrl(urlOrCode)
        if (code == null) {
            // Not a valid Echo code
            // Maybe handle external URL if we want? But user only asked for Echo Connection.
            return
        }

        viewModelScope.launch {
            updateState { it.copy(isLoading = true, isScanning = false) }
            
            val result = contactRepository.connectByReferral(code)
            
            result.onSuccess { referralResponse ->
                 updateState { it.copy(isLoading = false, result = referralResponse) }
            }.onFailure { e ->
                 updateState { it.copy(isLoading = false, error = "Failed to connect: ${e.message}", isScanning = false) } 
                 // Resume scanning on error? Or show error dialog?
            }
        }
    }
    
    fun onDismissResult() {
        updateState { it.copy(result = null, error = null, isScanning = true) }
    }
    
    fun unblockUser(userId: String) {
        viewModelScope.launch {
            try {
                // Fix: Use UserRepository to explicitly unblock the user.
                // ContactRepository.denyConnection only removes connections, it does not remove blocks.
                userRepository.unblockUser(userId)
                onDismissResult()
            } catch (e: Exception) {
                updateState { it.copy(error = "Failed to unblock: ${e.message}") }
            }
        }
    }
    
    private fun extractCodeFromUrl(url: String): String? {
        if (!url.contains("echo.app") && !url.contains("bhanitgaurav.com")) {
             // Treat as raw code if strict? Or return null?
             // If scans random text, we shouldn't spam backend.
             return null 
        }
        
        // Try query param 'code' (not currently used in my backend logic, I used `referralCode` in URL /r/CODE?)
        // Backend UserRepository uses User's `referralCode` field.
        // My Invite Logic in Repository: "https://api-echo.bhanitgaurav.com/r/$referralCode"
        // So Extract from last segment or query?
        // Uri parsing in KMP is tricky. Regex?
        
        // Regex for .../r/CODE...
        val regex = ".*/r/([A-Z0-9]+).*".toRegex()
        val match = regex.find(url)
        return match?.groupValues?.get(1) ?: run {
            // Check query param code=...
            val queryRegex = ".*[?&]code=([A-Z0-9]+).*".toRegex()
             queryRegex.find(url)?.groupValues?.get(1)
        }
    }
}

package com.bhanit.apps.echo.features.settings.presentation.settings

import androidx.lifecycle.viewModelScope
import com.bhanit.apps.echo.core.base.BaseViewModel
import com.bhanit.apps.echo.features.settings.domain.ContentRepository
import com.bhanit.apps.echo.core.network.AppException
import com.bhanit.apps.echo.core.network.safeApiCall
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class PoliciesState(
    val termsContent: String = "",
    val privacyContent: String = "",
    val isLoading: Boolean = true,
    val error: AppException? = null
)

class PoliciesViewModel(
    private val contentRepository: ContentRepository
) : BaseViewModel<PoliciesState>(PoliciesState()) {

    init {
        fetchPolicies()
    }

    fun reload() {
        fetchPolicies()
    }

    private fun fetchPolicies() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null) }
            
            // Use safeApiCall to handle exceptions standardly (e.g. SocketTimeout -> Offline message)
            // We run both in parallel but map them to Result using safeApiCall
            val termsDeferred = async { 
                safeApiCall { contentRepository.getContent("TERMS").first() }
            }
            val privacyDeferred = async { 
                safeApiCall { contentRepository.getContent("PRIVACY").first() }
            }
            
            val termsResult = termsDeferred.await()
            val privacyResult = privacyDeferred.await()
            
            // Check if both succeeded
            if (termsResult.isSuccess && privacyResult.isSuccess) {
                updateState { 
                    it.copy(
                        isLoading = false,
                        termsContent = termsResult.getOrDefault(""),
                        privacyContent = privacyResult.getOrDefault("")
                    ) 
                }
            } else {
                // Determine which error to show. Prioritize one.
                val exception = termsResult.exceptionOrNull() ?: privacyResult.exceptionOrNull()
                val appException = exception as? AppException 
                    ?: AppException(title = "Error", message = "Unable to load content")
                
                updateState { 
                    it.copy(
                        isLoading = false,
                        error = appException
                    ) 
                }
            }
        }
    }
}

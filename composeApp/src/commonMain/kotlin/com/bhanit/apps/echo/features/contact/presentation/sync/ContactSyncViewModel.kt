package com.bhanit.apps.echo.features.contact.presentation.sync

import com.bhanit.apps.echo.core.base.BaseViewModel
import com.bhanit.apps.echo.core.platform.PlatformContactService
import com.bhanit.apps.echo.features.contact.domain.ContactRepository
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope

data class ContactSyncState(
    val isLoading: Boolean = false,
    val contacts: List<ContactUiModel> = emptyList(),
    val error: String? = null
)

data class ContactUiModel(
    val name: String,
    val phone: String,
    val isRegistered: Boolean,
    val normalizedPhone: String
)

class ContactSyncViewModel(
    private val contactService: PlatformContactService,
    private val contactRepository: ContactRepository
) : BaseViewModel<ContactSyncState>(ContactSyncState()) {

    init {
        loadContacts()
    }

    fun loadContacts() {
        updateState { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                // 1. Get local contacts
                val platformContacts = contactService.getContacts()
                
                // 2. Map to Domain Contact
                val domainContacts = platformContacts.map { 
                    com.bhanit.apps.echo.features.contact.domain.Contact(
                        name = it.name,
                        phoneNumber = it.normalizedPhone
                    )
                }
                
                // 3. Sync with server
                val syncedContacts = contactRepository.syncContacts(domainContacts)
                
                // 4. Map to UI
                val uiModels = syncedContacts.map { 
                    ContactUiModel(
                        name = it.name,
                        phone = it.phoneNumber,
                        isRegistered = it.isRegistered,
                        normalizedPhone = it.phoneNumber
                    )
                }
                updateState { it.copy(isLoading = false, contacts = uiModels) }
            } catch (e: Exception) {
                updateState { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun invite(contact: ContactUiModel) {
        viewModelScope.launch {
            contactRepository.inviteUser(contact.normalizedPhone)
        }
    }
}

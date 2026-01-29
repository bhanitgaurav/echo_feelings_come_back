package com.bhanit.apps.echo.features.messaging.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhanit.apps.echo.features.messaging.domain.Message
import com.bhanit.apps.echo.features.messaging.domain.MessagingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.bhanit.apps.echo.core.network.AppException
import kotlinx.datetime.*
import com.bhanit.apps.echo.core.util.DateTimeUtils
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

data class EchoesState(
    val isLoading: Boolean = true,
    val isPaginationLoading: Boolean = false, // Added
    val echoesMessages: List<Message> = emptyList(),
    val groupedEchoes: Map<String, List<Message>> = emptyMap(),
    val replySuccess: Boolean = false,
    val error: AppException? = null,
    val replyStatus: Map<String, Boolean> = emptyMap(),
    val selectedFeelings: Set<com.bhanit.apps.echo.features.messaging.domain.Emotion> = emptySet(),
    val startDate: Long? = null,
    val endDate: Long? = null,
    val currentPage: Int = 1,
    val endReached: Boolean = false,
    val isValid: Boolean = true,
    val activeTab: Int = 0, // 0 = Inbox, 1 = My Echoes (Replies)
    val repliesMessages: List<Message> = emptyList(),
    val availableEmotions: List<com.bhanit.apps.echo.features.messaging.domain.Emotion> = emptyList(), // Added
    val contactsMap: Map<String, String> = emptyMap() // UserId -> Name
)

class EchoesViewModel(
    private val messagingRepository: MessagingRepository,
    private val emotionRepository: com.bhanit.apps.echo.features.messaging.domain.EmotionRepository,
    private val habitApi: com.bhanit.apps.echo.features.habits.data.HabitApi,
    private val contactRepository: com.bhanit.apps.echo.features.contact.domain.ContactRepository
) : ViewModel() {

    fun trackHomeScreenView() {
        viewModelScope.launch {
            try {
                habitApi.recordActivity("ECHOS_OPEN")
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private val _state = MutableStateFlow(EchoesState())
    val state: StateFlow<EchoesState> = _state.asStateFlow()

    init {
        loadAvailableEmotions()
        loadContacts()
    }

    private fun loadContacts() {
        viewModelScope.launch {
            contactRepository.getConnections()
                .collect { connections ->
                    val map = connections.associate { it.userId to it.username } // Default to 'username' from Connection, assuming 'name' isn't explicitly separate in Connection model or is aliased.
                    // Wait, Connection model has 'username'. Does it have 'name'? 
                    // Let's check Connection model again. It has userId, username, phoneNumber. 
                    // It does NOT have a separate 'Contact Name'.
                    // BUT, the goal is: "name of user as our contact name from the list... otherwise username".
                    // The Connection object likely represents the "Contact" link.
                    // If 'username' in Connection is actually the Display Name, we good.
                    // If not, we might need to check if 'Connection' has a local name override.
                    // User said: "contact name from the list".
                    // Connection model: data class Connection(userId, username, phoneNumber, status, avatarUrl).
                    // It seems 'username' IS the name we have for them?
                    // Or is there a 'Contact' entity?
                    // ContactRepository.syncContacts(contacts: List<Contact>) -> List<Contact>
                    // Contact entity has 'name'.
                    // But connections are 'Connection' objects.
                    // If I have them in my connections list, I probably see their Username?
                    // Or do I set a nickname? The code doesn't show nickname support yet.
                    // So "Contact Name" might just mean "Username of the connected user".
                    // The user said: "contact name from the list if its availiable otherwise username".
                    // This implies if they are in my contacts (Connections), show that name (which is username in Connection object).
                    // If NOT in contacts, show the username from the Message (receiverUsername).
                    
                    _state.update { it.copy(contactsMap = map) }
                }
        }
    }

    private fun loadAvailableEmotions() {
        viewModelScope.launch {
            val emotions = emotionRepository.getEmotions()
            _state.update { it.copy(availableEmotions = emotions) }
        }
    }

    fun setActiveTab(index: Int) {
        _state.update { it.copy(activeTab = index, error = null) }
        if (index == 0) {
            loadEchoes()
        } else {
            loadRepliedMessages()
        }
    }

    fun loadRepliedMessages(isNextPage: Boolean = false) {
        viewModelScope.launch {
            val currentState = state.value
            if (isNextPage && (currentState.endReached || currentState.isPaginationLoading)) return@launch

            val pageToLoad = if (isNextPage) currentState.currentPage + 1 else 1

            _state.update {
                it.copy(
                    isLoading = !isNextPage,
                    isPaginationLoading = isNextPage,
                    error = null
                )
            }

            try {
                // Fetch Sent Messages that have replies
                val result = messagingRepository.getSentMessages(
                    page = pageToLoad,
                    limit = 10,
                    onlyReplied = true
                )

                result.fold(
                    onSuccess = { messages ->
                         val isEndReached = messages.size < 10
                         
                         // Grouping
                         val (newMessages, grouped) = withContext(Dispatchers.Default) {
                             val combinedMessages = if (isNextPage) currentState.repliesMessages + messages else messages
                             val groupedResult = groupMessages(combinedMessages)
                             combinedMessages to groupedResult
                         }

                         _state.update { 
                             it.copy(
                                 repliesMessages = newMessages, // Store in separate list
                                 groupedEchoes = grouped, // Shared grouping for UI
                                 isLoading = false,
                                 isPaginationLoading = false,
                                 currentPage = pageToLoad,
                                 endReached = isEndReached
                             ) 
                         }
                    },
                    onFailure = { e ->
                        _state.update { it.copy(error = e as? AppException, isLoading = false, isPaginationLoading = false) }
                    }
                )
            } catch (e: Exception) {
                 _state.update { it.copy(error = null, isLoading = false, isPaginationLoading = false) }
            }
        }
    }

    fun loadEchoes(isNextPage: Boolean = false) {
        if (state.value.activeTab == 1) {
            loadRepliedMessages(isNextPage)
            return
        }
        viewModelScope.launch {
            val currentState = state.value
            if (isNextPage && (currentState.endReached || currentState.isPaginationLoading)) return@launch

            val pageToLoad = if (isNextPage) currentState.currentPage + 1 else 1

            _state.update {
                it.copy(
                    isLoading = !isNextPage, // Full screen load only for first page
                    isPaginationLoading = isNextPage,
                    error = null
                    // REMOVED: echoesMessages = emptyList... we keep old data to prevent flicker
                )
            }
            
            // Move network call to IO if not already (Repositories usually handle this, but safe to do so)
            // But definitely move Grouping to Default
            try {
                // Adjust Dates for Query (convert UTC midnight to Local Start/End of Day)
                val filterStartDate = currentState.startDate?.let { com.bhanit.apps.echo.core.util.DateTimeUtils.getStartOfDayInUserTimeZone(it) }
                val filterEndDate = currentState.endDate?.let { com.bhanit.apps.echo.core.util.DateTimeUtils.getEndOfDayInUserTimeZone(it) }
                
                // Assuming repository is main-safe or suspends.
                val result = messagingRepository.getInbox(
                    page = pageToLoad, 
                    limit = 10, 
                    feelings = if (currentState.selectedFeelings.isNotEmpty()) currentState.selectedFeelings.toList() else null,
                    startDate = filterStartDate,
                    endDate = filterEndDate
                )
                
                result.fold(
                    onSuccess = { messages ->
                         val isEndReached = messages.size < 10
                         
                         // HEAVY COMPUTATION ON BACKGROUND THREAD
                         val (newMessages, grouped) = withContext(Dispatchers.Default) {
                             val combinedMessages = if (isNextPage) currentState.echoesMessages + messages else messages
                             val groupedResult = groupMessages(combinedMessages)
                             combinedMessages to groupedResult
                         }

                         _state.update { 
                             it.copy(
                                 echoesMessages = newMessages, 
                                 groupedEchoes = grouped, 
                                 isLoading = false,
                                 isPaginationLoading = false,
                                 currentPage = pageToLoad,
                                 endReached = isEndReached
                             ) 
                         }
                         messages.forEach { msg -> checkReplyStatus(msg.id) }
                    },
                    onFailure = { e ->
                        _state.update { it.copy(error = e as? AppException, isLoading = false, isPaginationLoading = false) }
                    }
                )
            } catch (e: Exception) {
                 _state.update { it.copy(error = null, isLoading = false, isPaginationLoading = false) }
            }
        }
    }

    private fun groupMessages(messages: List<Message>): Map<String, List<Message>> {
        return try {
            val tz = TimeZone.currentSystemDefault()
            val now = Clock.System.now().toLocalDateTime(tz)
            val yesterday = Clock.System.now()
                .minus(1, DateTimeUnit.DAY, tz)
                .toLocalDateTime(tz)

            messages.groupBy { 
                DateTimeUtils.formatDateForHeader(it.timestamp, now, yesterday) 
            }
        } catch (e: Exception) {
            messages.groupBy { "Echoes" }
        }
    }

    fun toggleFeelingFilter(emotion: com.bhanit.apps.echo.features.messaging.domain.Emotion) {
        val current = state.value.selectedFeelings
        val newSet = if (current.contains(emotion)) {
            current - emotion
        } else {
            current + emotion
        }
        _state.update { it.copy(selectedFeelings = newSet) }
        loadEchoes()
    }

    fun setDateRange(start: Long?, end: Long?) {
        _state.update { it.copy(startDate = start, endDate = end) }
        loadEchoes()
    }

    fun clearFilters() {
        _state.update { it.copy(selectedFeelings = emptySet(), startDate = null, endDate = null) }
        loadEchoes()
    }

    private fun checkReplyStatus(messageId: String) {
        viewModelScope.launch {
            try {
                val result = messagingRepository.checkReplyStatus(messageId)
                result.onSuccess { canReply ->
                    _state.update { 
                        val newStatus = it.replyStatus.toMutableMap()
                        newStatus[messageId] = canReply
                        it.copy(replyStatus = newStatus)
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun replyToMessage(messageId: String, content: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, replySuccess = false, error = null) }
            
            val result = messagingRepository.replyToMessage(messageId, content)
            result.fold(
                onSuccess = {
                     _state.update { it.copy(isLoading = false, replySuccess = true) }
                     loadEchoes() // Refresh to show reply logic if any
                     // Update status to false locally
                     _state.update { 
                         val newStatus = it.replyStatus.toMutableMap()
                         newStatus[messageId] = false
                         it.copy(replyStatus = newStatus)
                     }
                },
                onFailure = { e ->
                    _state.update { it.copy(isLoading = false, error = e as? AppException) }
                }
            )
        }
    }
    
    fun resetState() {
         _state.update { it.copy(replySuccess = false, error = null) }
    }
}

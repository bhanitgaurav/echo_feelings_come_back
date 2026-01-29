package com.bhanit.apps.echo.features.credits.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import com.bhanit.apps.echo.data.model.PurchaseType
import org.koin.compose.viewmodel.koinViewModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferralScreen(
    onBack: () -> Unit
) {
    val viewModel = koinViewModel<CreditsViewModel>()
    val shareManager = org.koin.compose.koinInject<com.bhanit.apps.echo.core.util.ShareManager>()
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Refer & Earn") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            com.bhanit.apps.echo.core.presentation.components.LogicWrapper(
                isLoading = state.isLoading,
                error = state.error,
                onRetry = viewModel::loadData,
                loadingContent = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            ) {
                ReferTab(
                    credits = state.credits,
                    referralCode = state.referralCode,
                    referralUrl = state.referralUrl,
                    onShare = { code, url -> 
                         val dynamicUrl = if (!url.isNullOrEmpty()) {
                                if (url.contains("?")) "$url&m=settings" else "$url?m=settings"
                         } else {
                                "https://api-echo.bhanitgaurav.com/r/$code?m=settings"
                         }
                        
                        val shareText = "Join me on Echo! Use my referral code to get started: $code\n\nDownload: $dynamicUrl"
                        shareManager.shareText(shareText)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditPremiumScreen(
    initialTab: Int = 0,
    onBack: () -> Unit
) {
    val viewModel = koinViewModel<CreditsViewModel>()
    val state by viewModel.state.collectAsState()
    var selectedTab by remember { mutableStateOf(initialTab) }
    val tabs = listOf("Premium", "Journey", "Credits") // Inserted Journey

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Premium & Credits") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            com.bhanit.apps.echo.core.presentation.components.LogicWrapper(
                isLoading = state.isLoading && state.history.isEmpty(), // LogicWrapper mostly checks loading
                error = state.error,
                onRetry = viewModel::loadData,
                loadingContent = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            ) {
                androidx.compose.animation.Crossfade(targetState = selectedTab) { tabIndex ->
                    when (tabIndex) {
                        0 -> ShopTab(
                            emotions = state.emotions,
                            phoneNumber = state.phoneNumber,
                            onPurchase = { type, id, count -> viewModel.purchaseItem(type, id, count) }
                        )
                        1 -> JourneyTab(
                            milestones = state.milestones
                        )
                        2 -> CreditTransactionTab(
                            credits = state.credits,
                            history = state.history,
                            searchQuery = state.searchQuery,
                            activeFilter = state.activeFilter,
                            startDate = state.startDate,
                            endDate = state.endDate,
                            isHistoryLoading = state.isHistoryLoading,
                            onSearch = viewModel::onSearch,
                            onFilter = viewModel::onFilter,
                            onDateRangeSelected = viewModel::onDateRangeSelected,
                            onLoadMore = viewModel::loadNextPage
                        )
                    }
                }
            }

            val purchaseSuccess = state.purchaseSuccess
            if (purchaseSuccess != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.clearMessage() },
                    title = { Text("Success") },
                    text = { Text(purchaseSuccess) },
                    confirmButton = { TextButton(onClick = { viewModel.clearMessage() }) { Text("OK") } }
                )
            }
            
            val purchaseError = state.purchaseError
            if (purchaseError != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.clearMessage() },
                    title = { Text("Purchase Failed") },
                    text = { Text(purchaseError) },
                    confirmButton = { TextButton(onClick = { viewModel.clearMessage() }) { Text("OK") } }
                )
            }
        }
    }
}

// ... ReferTab stays same ...
@Composable
fun ReferTab(credits: Int, referralCode: String?, referralUrl: String?, onShare: (String, String?) -> Unit) {
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var showCopiedMessage by remember { mutableStateOf(false) }

    // Animation state for the gift icon gradient scale
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Background Circle 1 Animation (Blue)
    val circle1OffsetX by infiniteTransition.animateFloat(
        initialValue = -30f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val circle1OffsetY by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = 50f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Background Circle 2 Animation (Magenta)
    val circle2OffsetX by infiniteTransition.animateFloat(
        initialValue = 50f,
        targetValue = -350f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val circle2OffsetY by infiniteTransition.animateFloat(
        initialValue = 40f,
        targetValue = -340f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Background Circle 2 Animation (Yellow)
    val circle3OffsetX by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = -550f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val circle3OffsetY by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = -540f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background Decorative Elements
        Canvas(modifier = Modifier.fillMaxSize().alpha(0.15f)) {
            // Blue Circle (Top Leftish)
            drawCircle(
                color = androidx.compose.ui.graphics.Color.Blue, 
                radius = 250f, 
                center = center.copy(
                    x = center.x - 200 + circle1OffsetX, 
                    y = center.y - 300 + circle1OffsetY
                )
            )
            // Magenta Circle (Bottom Rightish)
            drawCircle(
                color = androidx.compose.ui.graphics.Color.Magenta, 
                radius = 200f, 
                center = center.copy(
                    x = center.x + 200 + circle2OffsetX, 
                    y = center.y + 100 + circle2OffsetY
                )
            )
            // Yellow Circle (Bottom Rightish)
            drawCircle(
                color = androidx.compose.ui.graphics.Color.Yellow,
                radius = 150f,
                center = center.copy(
                    x = center.x + 150 + circle3OffsetX,
                    y = center.y + 90 + circle3OffsetY
                )
            )
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Balance Section
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp).alpha(0.1f),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total Credits", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = "$credits",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Main Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Invite Friends",
                        modifier = Modifier.size(48.dp).graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        },
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Invite Friends & Earn", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Share your code and earn 10 credits for every friend who joins!",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    if (referralCode != null) {
                        Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth().clickable {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(referralCode))
                                showCopiedMessage = true
                                // onShare(referralCode, referralUrl) // Optional: Share on tap? Let's keep it copy-only for better UX
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Your Code", style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        text = referralCode,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = { onShare(referralCode, referralUrl) }) {
                                    Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        
                        AnimatedVisibility(showCopiedMessage) {
                            Text(
                                "Code copied!",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = { onShare(referralCode, referralUrl) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share Link")
                        }
                        
                        TextButton(onClick = {
                             clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(referralCode))
                             showCopiedMessage = true
                        }) {
                             Text("Copy Code")
                        }
                    } else {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ShopTab(
    emotions: List<com.bhanit.apps.echo.features.messaging.domain.Emotion>,
    phoneNumber: String?,
    onPurchase: (PurchaseType, String, Int?) -> Unit
) {
    var emotionToPurchase by remember { mutableStateOf<com.bhanit.apps.echo.features.messaging.domain.Emotion?>(null) }
    
    // Apple Review Requirement for Test Number
    val isTestUser = phoneNumber?.replace("+", "")?.endsWith("9988776655") == true

    if (emotionToPurchase != null) {
        AlertDialog(
            onDismissRequest = { emotionToPurchase = null },
            title = { Text("Unlock Emotion") },
            text = { Text("Unlock '${emotionToPurchase?.displayName}' for ${emotionToPurchase?.price} credits associated with 7 days?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val emotion = emotionToPurchase
                        if (emotion != null) {
                            onPurchase(PurchaseType.EMOTION_UNLOCK, emotion.id, null)
                        }
                        emotionToPurchase = null
                    }
                ) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { emotionToPurchase = null }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Boost Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Row(
                   modifier = Modifier.padding(16.dp).fillMaxWidth(),
                   verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Star, contentDescription = null)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Boost Your Limits", style = MaterialTheme.typography.labelLarge)
                        Text(
                           "Running out of echoes? Boost sending limits when you hit the cap!", 
                           style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Sorting Logic:
        // 1. Locked & Premium/Premium+ (Top) (Sorted by price asc)
        // 2. Unlocked & Premium/Premium+ (Middle) (Sorted by price asc)
        // 3. Free (Bottom)
        
        val premiumLocked = emotions.filter { !it.isUnlocked && it.tier != "FREE" }.sortedBy { it.price }
        val premiumUnlocked = emotions.filter { it.isUnlocked && it.tier != "FREE" }.sortedBy { it.price }
        val freeEmotions = emotions.filter { it.tier == "FREE" } // Always unlocked

        if (premiumLocked.isNotEmpty()) {
            item {
                if (isTestUser) {
                   Text(
                       "Credits are earned through referrals and daily usage. Purchases are not available.",
                       style = MaterialTheme.typography.bodyMedium,
                       color = MaterialTheme.colorScheme.primary,
                       modifier = Modifier.padding(top=8.dp, bottom = 8.dp)
                   )
                }
                Text("Premium Emotions", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 8.dp))
                Text("Unlock for 7 Days", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            }
            items(premiumLocked) { emotion ->
                EmotionStoreItem(emotion, onPurchase = { _, _, _ -> emotionToPurchase = emotion })
            }
        } else if (premiumUnlocked.isNotEmpty()) {
             // If all premium are unlocked, maybe show a "You have everything!" text?
             item {
                 Text("Premium Emotions", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 8.dp))
                 Text("All premium emotions unlocked! 🎉", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
             }
        }

        if (premiumUnlocked.isNotEmpty()) {
             if (premiumLocked.isNotEmpty()) {
                 // Only show header if we have a mix. If all unlocked, header is above.
                 item {
                     Text("Unlocked Premium", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
                 }
             }
             items(premiumUnlocked) { emotion ->
                 EmotionStoreItem(emotion, onPurchase = { _, _, _ -> }) // Already unlocked
             }
        }

        if (freeEmotions.isNotEmpty()) {
             item {
                  Text("Free Emotions", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
                  Text("Always available", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
             }
             items(freeEmotions) { emotion ->
                 EmotionStoreItem(emotion, onPurchase = { _, _, _ -> }) // Free
             }
        }
    }
}

@Composable
fun EmotionStoreItem(
    emotion: com.bhanit.apps.echo.features.messaging.domain.Emotion,
    onPurchase: (PurchaseType, String, Int?) -> Unit
) {
     Card(
         modifier = Modifier.fillMaxWidth(),
         shape = MaterialTheme.shapes.medium
     ) {
         Row(
             modifier = Modifier.padding(16.dp).fillMaxWidth(),
             horizontalArrangement = Arrangement.SpaceBetween,
             verticalAlignment = Alignment.CenterVertically
         ) {
             Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                 // Color Circle
                 Surface(
                     modifier = Modifier.size(40.dp),
                     shape = androidx.compose.foundation.shape.CircleShape,
                     color = androidx.compose.ui.graphics.Color(emotion.colorHex.toInt())
                 ) {}
                 Spacer(modifier = Modifier.width(16.dp))
                 Column {
                     Text(emotion.displayName, style = MaterialTheme.typography.titleMedium)
                     Text(emotion.tier, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                     
                     if (emotion.isUnlocked && emotion.expiresAt != null && emotion.tier != "FREE") {
                         val dateText = com.bhanit.apps.echo.core.util.DateTimeUtils.formatDateShort(emotion.expiresAt)
                         Text("Expires: $dateText", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                     }
                 }
             }

             if (emotion.isUnlocked) {
                 Icon(
                     imageVector = androidx.compose.material.icons.Icons.Default.Star,
                     contentDescription = "Unlocked",
                     tint = MaterialTheme.colorScheme.primary
                 )
             } else {
                 Button(
                     onClick = { onPurchase(PurchaseType.EMOTION_UNLOCK, emotion.id, null) },
                 ) {
                     Text("${emotion.price} Credits")
                 }
             }
         }
     }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditTransactionTab(
    credits: Int,
    history: List<com.bhanit.apps.echo.data.model.CreditTransaction>,
    searchQuery: String,
    activeFilter: com.bhanit.apps.echo.data.model.TransactionFilter,
    startDate: Long?,
    endDate: Long?,
    isHistoryLoading: Boolean,
    onSearch: (String) -> Unit,
    onFilter: (com.bhanit.apps.echo.data.model.TransactionFilter) -> Unit,
    onDateRangeSelected: (Long?, Long?) -> Unit,
    onLoadMore: () -> Unit
) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    var isSearchFocused by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            },
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Enhanced Credit Card
        item(key = "credit_card") {
            AnimatedVisibility(
                visible = !isSearchFocused,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                EnhancedCreditCard(credits)
            }
        }

        // Search and Filter
        item(key = "search_filter") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearch,
                    placeholder = { Text("Search transactions...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isSearchFocused = it.isFocused },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(androidx.compose.material.icons.Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearch("") }) {
                                Icon(androidx.compose.material.icons.Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
                
                if (isHistoryLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    com.bhanit.apps.echo.data.model.TransactionFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = activeFilter == filter,
                            onClick = { onFilter(filter) },
                            label = { Text(filter.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            leadingIcon = if (activeFilter == filter) {
                                { Icon(androidx.compose.material.icons.Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }

                    // Date Range Filter
                    FilterChip(
                        selected = startDate != null || endDate != null,
                        onClick = { showDatePicker = true },
                        label = { Text("Date") },
                        leadingIcon = { Icon(androidx.compose.material.icons.Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        trailingIcon = if (startDate != null || endDate != null) {
                            {
                                IconButton(onClick = { onDateRangeSelected(null, null) }, modifier = Modifier.size(16.dp)) {
                                    Icon(androidx.compose.material.icons.Icons.Default.Close, contentDescription = "Clear Date", modifier = Modifier.size(12.dp))
                                }
                            }
                        } else null
                    )
                }
                
                if (showDatePicker) {
                    val datePickerState = rememberDateRangePickerState(
                        initialSelectedStartDateMillis = startDate,
                        initialSelectedEndDateMillis = endDate
                    )
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                onDateRangeSelected(datePickerState.selectedStartDateMillis, datePickerState.selectedEndDateMillis)
                                showDatePicker = false
                            }) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                        }
                    ) {
                        DateRangePicker(
                            state = datePickerState,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        
        item(key = "history_header") {
             Text(
                 "Transaction History", 
                 style = MaterialTheme.typography.titleMedium, 
                 fontWeight = FontWeight.Bold,
                 modifier = Modifier.padding(vertical = 8.dp)
             )
        }

        if (history.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No transactions found", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                }
            }
        } else {

            items(
                items = history,
                key = { it.id }
            ) { transaction ->
                ListItem(
                    headlineContent = { 
                        val typeName = transaction.type.name
                        val displayText = if (typeName == "MANUAL_ADJUSTMENT" || typeName == "MANUAL_GRANT") {
                            "Admin Adjustment"
                        } else {
                            typeName.replace("_", " ")
                        }
                        Text(displayText) 
                    },
                    supportingContent = { Text(transaction.description) },
                    trailingContent = {
                        val color = if (transaction.amount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        Text(
                            text = (if (transaction.amount > 0) "+" else "") + "${transaction.amount}",
                            color = color,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    overlineContent = {
                        val formattedTime = com.bhanit.apps.echo.core.util.DateTimeUtils.formatMessageTime(transaction.timestamp)
                        Text(formattedTime)
                    }
                )
                HorizontalDivider()
                
                // Trigger load more if near end
                if (history.lastOrNull() == transaction) {
                    LaunchedEffect(Unit) {
                         onLoadMore()
                    }
                }
            }
        }

    }
}

@Composable
fun EnhancedCreditCard(credits: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                )
        ) {
            // Decorative Circles
            Canvas(modifier = Modifier.fillMaxSize().alpha(0.1f)) {
                drawCircle(
                    color = androidx.compose.ui.graphics.Color.White,
                    radius = 300f,
                    center = androidx.compose.ui.geometry.Offset(x = size.width, y = 0f)
                )
                drawCircle(
                    color = androidx.compose.ui.graphics.Color.White,
                    radius = 150f,
                    center = androidx.compose.ui.geometry.Offset(x = 0f, y = size.height)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Echo Premium",
                        style = MaterialTheme.typography.titleMedium,
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = "Available Credits",
                        style = MaterialTheme.typography.labelMedium,
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "$credits",
                        style = MaterialTheme.typography.displayMedium,
                        color = androidx.compose.ui.graphics.Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

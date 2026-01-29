package com.bhanit.apps.echo.features.welcome

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Diversity1
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhanit.apps.echo.core.designsystem.components.EchoButton
import org.koin.compose.viewmodel.koinViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(
    onNavigateToLogin: () -> Unit
) {
    val viewModel = koinViewModel<WelcomeViewModel>()
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()

    val pages = listOf(
        WelcomePage(
            title = "Connect More Meaningfully",
            description = "Share feelings with people you already know — friends, family, coworkers.\n\nConnect instantly by scanning their Echo QR — no phone numbers needed.",
            icon = Icons.Default.Diversity1,
            color = Color(0xFF10B981), // Emerald
            content = { MockMutualConnectionCard() }
        ),
        WelcomePage(
            title = "Anonymous & Safe",
            description = "They see the feeling — not your name.\n\nDesigned to respect privacy and emotional safety.",
            icon = Icons.Default.Security,
            color = Color(0xFFEC4899), // Pink
            content = { MockAnonymousInboxCard() }
        ),
        WelcomePage(
            title = "Say What You Feel",
            description = "No replies needed. \nNo awkward conversations.\nJust honest feelings.",
            icon = Icons.Default.Favorite,
            color = Color(0xFF8B5CF6), // Violet
            content = { MockSendingEchoCard() }
        ),
        WelcomePage(
            title = "How Echo Works",
            description = "Simple. Honest. Human.",
            icon = Icons.Default.VolunteerActivism, // Or a generic 'Info' or 'Sparkle' icon
            color = MaterialTheme.colorScheme.primary,
            content = { MockHowItWorksCard() }
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { pageIndex ->
                WelcomePageContent(pages[pageIndex])
            }

            // Indicators & Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Indicators
                Row(
                    modifier = Modifier.padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(pages.size) { iteration ->
                        val color = if (pagerState.currentPage == iteration) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }

                // Button
                val isLastPage = pagerState.currentPage == pages.size - 1
                
                EchoButton(
                    text = if (isLastPage) "Get Started" else "Continue",
                    onClick = {
                        if (isLastPage) {
                            onNavigateToLogin()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    icon = if (!isLastPage) Icons.Default.ArrowForward else null
                )
                
                TextButton(
                    onClick = if (!isLastPage) onNavigateToLogin else { {} },
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .alpha(if (!isLastPage) 1f else 0f),
                    enabled = !isLastPage
                ) {
                    Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun WelcomePageContent(page: WelcomePage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        Icon(
            imageVector = page.icon,
            contentDescription = null,
            modifier = Modifier.size(100.dp).padding(bottom = 24.dp),
            tint = page.color
        )
        
        // Content Area (Mock UI)
        Box(
            modifier = Modifier
                .height(260.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            page.content()
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// --- Mock Components for Visual Samples ---

@Composable
fun MockHowItWorksCard() {
    Card(
        modifier = Modifier
            .width(300.dp)
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            HowItWorksItem(
                icon = Icons.Default.Favorite,
                text = "Send a feeling to someone you know.",
                color = Color(0xFF8B5CF6)
            )
            HowItWorksItem(
                icon = Icons.Default.Security,
                text = "Your name stays hidden.",
                color = Color(0xFFEC4899)
            )
            HowItWorksItem(
                icon = Icons.Default.VolunteerActivism, // Reflect icon metaphor
                text = "They can reflect — or echo back if they want.",
                color = Color(0xFF10B981)
            )
        }
    }
}

@Composable
fun HowItWorksItem(icon: ImageVector, text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}


@Composable
fun MockSendingEchoCard() {
    Card(
        modifier = Modifier
            .width(320.dp)
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: To
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("To:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("@Sarah", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Emotion Selection
            Text("Select Emotion", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Unselected - Like (Blue)
                Icon(Icons.Default.ThumbUp, null, tint = Color(0xFF3B82F6).copy(alpha = 0.6f), modifier = Modifier.size(24.dp))
                
                // Selected - Heart (Violet/Purple from theme)
                Box(modifier = Modifier.size(32.dp).background(Color(0xFF8B5CF6), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Favorite, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                
                // Unselected - Star (Amber)
                Icon(Icons.Default.Star, null, tint = Color(0xFFF59E0B).copy(alpha = 0.6f), modifier = Modifier.size(24.dp))
                
                // Unselected - Care (Pink/Red)
                Icon(Icons.Default.VolunteerActivism, null, tint = Color(0xFFEF4444).copy(alpha = 0.6f), modifier = Modifier.size(24.dp))
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Input Area
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Your help today meant a lot to me. ✨",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(Modifier.width(8.dp))
                
                // Send Button
                Box(
                    modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ArrowForward, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun MockAnonymousInboxCard() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Item 1: Received
        Card(
             modifier = Modifier.width(260.dp),
             colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
             elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
             shape = RoundedCornerShape(16.dp)
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).background(Color(0xFFEC4899), CircleShape), contentAlignment = Alignment.Center) {
                     Icon(Icons.Default.Security, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Anonymous", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("Someone appreciated your effort.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.weight(1f))
                Surface(
                    color = Color(0xFFEC4899).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("SECRET", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(4.dp), color = Color(0xFFEC4899), fontSize = 10.sp)
                }
            }
        }
        
         // Item 2: Another Received
        Card(
             modifier = Modifier.width(260.dp).alpha(0.6f),
             colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
             elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
             shape = RoundedCornerShape(16.dp)
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.secondary, CircleShape), contentAlignment = Alignment.Center) {
                     Text("?", color = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Someone", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("Feeling...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun MockMutualConnectionCard() {
    Card(
        modifier = Modifier
            .width(260.dp)
            .padding(8.dp),
        colors = CardDefaults.cardColors(
             containerColor = MaterialTheme.colorScheme.surface
        ),
         elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
         shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Visual Connector
            Box(Modifier.height(60.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                 // Line
                 Box(Modifier.width(100.dp).height(2.dp).background(MaterialTheme.colorScheme.outlineVariant))
                 
                 // Avatars
                 Row(
                     horizontalArrangement = Arrangement.SpaceBetween,
                     verticalAlignment = Alignment.CenterVertically,
                     modifier = Modifier.width(140.dp)
                 ) {
                     Box(Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary, CircleShape).border(2.dp, MaterialTheme.colorScheme.surface, CircleShape), contentAlignment = Alignment.Center) {
                         Text("You", color = Color.White, style = MaterialTheme.typography.labelSmall)
                     }
                      Box(Modifier.size(32.dp).background(Color(0xFF10B981), CircleShape), contentAlignment = Alignment.Center) {
                         Icon(Icons.Default.ThumbUp, null, tint = Color.White, modifier = Modifier.size(16.dp))
                     }
                     Box(Modifier.size(48.dp).background(MaterialTheme.colorScheme.secondary, CircleShape).border(2.dp, MaterialTheme.colorScheme.surface, CircleShape), contentAlignment = Alignment.Center) {
                         Text("Alex", color = Color.White, style = MaterialTheme.typography.labelSmall)
                     }
                 }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Text("You are Connected!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
            Spacer(Modifier.height(4.dp))
            Text(
                "You and Alex can now echo each other.", 
                style = MaterialTheme.typography.bodySmall, 
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

data class WelcomePage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val content: @Composable () -> Unit
)

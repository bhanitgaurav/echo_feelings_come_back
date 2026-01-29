package com.bhanit.apps.echo.features.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.bhanit.apps.echo.core.designsystem.components.EchoCard
import com.bhanit.apps.echo.core.navigation.DashboardRoute
import com.bhanit.apps.echo.core.navigation.EchoesRoute
import com.bhanit.apps.echo.core.navigation.PeopleRoute
import com.bhanit.apps.echo.core.navigation.SettingsRoute
import com.bhanit.apps.echo.core.theme.ColorSchemeGlass


import echo.composeapp.generated.resources.Res
import echo.composeapp.generated.resources.echo_base
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

data class TopLevelRoute<T : Any>(val name: String, val route: T, val icon: Any)

@Composable
fun MainScaffold(
    navController: androidx.navigation.NavHostController,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val topLevelRoutes = listOf(
        TopLevelRoute("Echo", DashboardRoute, Icons.Default.GraphicEq),
        TopLevelRoute("Echoes", EchoesRoute, Res.drawable.echo_base),
        TopLevelRoute("People", PeopleRoute, Icons.Default.People),
        TopLevelRoute("Settings", SettingsRoute, Icons.Default.Settings)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent
        ) { innerPadding ->
            content(innerPadding)
        }
        // Floating Glass Bottom Navigation
        EchoCard(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.navigationBars) // Handle bottom inset
                .fillMaxWidth(),
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant, // Theme-aware glass
            elevation = 0.dp, // Remove elevation to eliminate shadow/tint artifacts
            border = null, // Remove default frost border to eliminate "white divider" artifact
            shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp), // Slightly shorter for cleaner look
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                topLevelRoutes.forEach { topLevelRoute ->
                    val isSelected =
                        currentDestination?.hasRoute(topLevelRoute.route::class) == true
                    
                    // Custom Navigation Item
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null // Explicitly null to avoid any ripple causing artifacts for now, as I suspect ripple might clip poorly. 
                                // To restore animation, I rely on the Text/Icon sizing animation I added.
                            ) {
                                navController.navigate(topLevelRoute.route) {
                                    // Pop up to the start destination of the graph to
                                    // avoid building up a large stack of destinations
                                    // on the back stack as users select items
                                    popUpTo(DashboardRoute) {
                                        saveState = true
                                    }
                                    // Avoid multiple copies of the same destination when
                                    // reselecting the same item
                                    launchSingleTop = true
                                    // Restore state when reselecting a previously selected item
                                    restoreState = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Icon with scale animation potential, but let's keep it simple first
                            val icon = topLevelRoute.icon
                            val tint = if (isSelected) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            
                            if (icon is androidx.compose.ui.graphics.vector.ImageVector) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = topLevelRoute.name,
                                    tint = tint,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else if (icon is DrawableResource) {
                                Icon(
                                    painter = painterResource(icon),
                                    contentDescription = topLevelRoute.name,
                                    tint = tint,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            // Text Label with Animation
                            AnimatedVisibility(
                                visible = isSelected,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Text(
                                    text = topLevelRoute.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

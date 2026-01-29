package com.bhanit.apps.echo.core.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bhanit.apps.echo.core.network.AppException

/**
 * A standardized wrapper for handling UI states: Loading (Shimmer) -> Error -> Content.
 *
 * @param isLoading Whether the data is currently loading.
 * @param error The current error state, if any.
 * @param onRetry Callback for the retry action in the error view.
 * @param loadingContent Composable to show when loading (e.g., Shimmer).
 * @param content Composable to show when data is successfully loaded.
 */
@Composable
fun LogicWrapper(
    isLoading: Boolean,
    error: AppException?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    loadingContent: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    // Priority: Error > Loading > Content
    // Or Loading > Error > Content?
    // User request: "shimmer in all screens... handle it like states"
    // Usually:
    // 1. If Loading && No Data -> Show Shimmer
    // 2. If Error && No Data -> Show Error
    // 3. If Data (regardless of loading/error for pull-to-refresh) -> Show Content 
    //    (But we agreed to clear stale data, so Data is cleared on load)
    // Therefore:
    // 1. If Loading -> Show Shimmer
    // 2. If Error -> Show Error
    // 3. Else -> Show Content
    
    AnimatedContent(
        targetState = Triple(isLoading, error, Unit),
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
        },
        label = "LogicWrapperState",
        modifier = modifier
    ) { (loading, err, _) ->
        when {
            loading -> {
                loadingContent()
            }
            err != null -> {
                EchoErrorView(
                    error = err,
                    onRetry = onRetry
                )
            }
            else -> {
                content()
            }
        }
    }
}

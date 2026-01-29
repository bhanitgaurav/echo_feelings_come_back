package com.bhanit.apps.echo.core.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SwipeToRevealCard(
    isRevealed: Boolean,
    onRevealChange: (Boolean) -> Unit,
    onShareClick: () -> Unit,
    revealedContent: @Composable BoxScope.() -> Unit,
    isSwipeEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    // How far we can swipe left (reveal width). E.g., 80.dp
    val revealSize = 80.dp
    val revealPx = with(density) { revealSize.toPx() }
    
    // Offset of the content card. 0 = closed, -revealPx = revealed
    val state = remember { Animatable(if (isRevealed) -revealPx else 0f) }
    val scope = rememberCoroutineScope()

    // Sync external state changes
    androidx.compose.runtime.LaunchedEffect(isRevealed) {
        val target = if (isRevealed) -revealPx else 0f
        state.animateTo(target)
    }

    if (!isSwipeEnabled) {
        Box { content() }
        return
    }

    // Draggable Logic
    // We want to drag LEFT (negative values).
    val draggableState = rememberDraggableState { delta ->
        val newValue = (state.value + delta).coerceIn(-revealPx, 0f)
        scope.launch { state.snapTo(newValue) }
    }

    Box {
        // 1. Background Action (Revealed Content)
        // Visible when swipe happens
        // Alpha calculation: 0 at offset 0, 1 at offset -revealPx
        val revealAlpha = (state.value / -revealPx).coerceIn(0f, 1f)
        
        Box(
            modifier = Modifier
                .matchParentSize()
                .alpha(revealAlpha), // Fade in on swipe
            contentAlignment = Alignment.CenterEnd
        ) {
            revealedContent()
        }

        // 2. Foreground Content
        Box(
            modifier = Modifier
                .offset { IntOffset(state.value.roundToInt(), 0) }
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = { velocity ->
                        // Snap logic
                        val target = if (state.value < -revealPx / 2 || velocity < -1000) {
                            -revealPx // Snap to Open
                        } else {
                            0f // Snap to Close
                        }
                        
                        val shouldBeRevealed = target == -revealPx
                        if (shouldBeRevealed != isRevealed) {
                            onRevealChange(shouldBeRevealed)
                        } else {
                             // If state didn't change (e.g. was already open and user dragged a bit but didn't close),
                             // ensure we snap back to the correct visual position for that state
                             scope.launch { state.animateTo(target) }
                        }
                    }
                )
        ) {
            content()
        }
    }
}

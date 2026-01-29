package com.bhanit.apps.echo.core.tutorial

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

data class CoachMarkTarget(
    val id: String,
    val rect: Rect,
    val description: String,
    val title: String
)

class CoachMarkController(
    private val repository: CoachMarkRepository,
    private val scope: CoroutineScope
) {
    var activeTarget by mutableStateOf<CoachMarkTarget?>(null)
        private set

    private val measuredTargets = mutableMapOf<String, LayoutCoordinates>()
    private val pendingTutorials = mutableListOf<CoachMarkData>()

    fun registerTarget(id: String, coordinates: LayoutCoordinates) {
        measuredTargets[id] = coordinates
        checkPending()
    }

    fun showTutorial(data: CoachMarkData) {
        if (activeTarget != null) return // Already showing one? Queueing logic could be added here
        pendingTutorials.add(data)
        checkPending()
    }
    
    fun dismissCurrent() {
        val current = activeTarget
        activeTarget = null
        if (current != null) {
            scope.launch {
                repository.markTutorialSeen(current.id)
            }
        }
        // Could show next in queue here
        checkPending()
    }

    private fun checkPending() {
        if (pendingTutorials.isEmpty()) return
        
        // Sort by order so lower order shows first
        val next = pendingTutorials.sortedBy { it.order }.first()
        val coordinates = measuredTargets[next.targetId]
        
        if (coordinates != null && coordinates.isAttached) {
            val position = coordinates.positionInRoot()
            val size = coordinates.size
            
            activeTarget = CoachMarkTarget(
                id = next.id,
                rect = Rect(position.x, position.y, position.x + size.width, position.y + size.height),
                title = next.title,
                description = next.description
            )
            pendingTutorials.remove(next)
        }
    }
}

data class CoachMarkData(
    val id: String,
    val targetId: String,
    val title: String,
    val description: String,
    val order: Int = 0
)

val LocalCoachMarkController = compositionLocalOf<CoachMarkController> {
    error("No CoachMarkController provided")
}

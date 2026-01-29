package com.bhanit.apps.echo.core.tutorial

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned

@Composable
fun Modifier.coachMark(
    id: String,
    title: String,
    description: String,
    order: Int = 0
): Modifier {
    val controller = LocalCoachMarkController.current
    val repository = LocalCoachMarkRepository.current // We need to expose Repo too or Controller handles it?
    
    // Better: Controller handles state check inside showTutorial logic using repository
    // But we need to register coordinates always
    
    // Check if seen
    LaunchedEffect(id) {
         // Logic to trigger:
         // Controller should expose a way to "queue" this tutorial.
         // If we do it here, every recomposition might re-queue.
         
         // Let's rely on onGloballyPositioned for registration, 
         // and a side-effect to tell controller "I exist and might need showing"
         
         repository.isTutorialSeen(id).collect { seen ->
             if (!seen) {
                 controller.showTutorial(
                     CoachMarkData(
                         id = id,
                         targetId = id,
                         title = title,
                         description = description,
                         order = order
                     )
                 )
             }
         }
    }

    return this.onGloballyPositioned { coordinates ->
        controller.registerTarget(id, coordinates)
    }
}

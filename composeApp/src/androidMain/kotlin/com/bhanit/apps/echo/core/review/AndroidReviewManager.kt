package com.bhanit.apps.echo.core.review

import android.app.Activity
import android.content.Context
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.tasks.await

class AndroidReviewManager(private val context: Context) : ReviewManager {
    override suspend fun tryRequestReview() {
        val activity = com.bhanit.apps.echo.core.util.CurrentActivityHolder.currentActivity
        println("ReviewManager: Requesting review. Activity found: ${activity?.javaClass?.simpleName}")
        
        if (activity == null) {
            println("ReviewManager: Aborting. No current activity.")
            return
        }
        
        val manager = ReviewManagerFactory.create(context)
        try {
            val reviewInfo = manager.requestReviewFlow().await()
            println("ReviewManager: Review info received. Launching flow.")
            manager.launchReviewFlow(activity, reviewInfo).await()
            println("ReviewManager: Review flow completed.")
        } catch (e: Exception) {
            println("ReviewManager: Error - ${e.message}")
            e.printStackTrace()
        }
    }
}

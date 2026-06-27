package com.wkq.google.rate

import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory

internal object GooglePlayReviewLauncher {

    fun launch(activity: Activity, onComplete: (Boolean) -> Unit) {
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow()
            .addOnSuccessListener { reviewInfo ->
                manager.launchReviewFlow(activity, reviewInfo)
                    .addOnCompleteListener { onComplete(true) }
                    .addOnFailureListener { onComplete(false) }
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }
}

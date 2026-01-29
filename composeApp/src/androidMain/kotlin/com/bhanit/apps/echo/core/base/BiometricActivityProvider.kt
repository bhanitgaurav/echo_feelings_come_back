package com.bhanit.apps.echo.core.base

import androidx.fragment.app.FragmentActivity
import java.lang.ref.WeakReference

object BiometricActivityProvider {
    private var activityRef: WeakReference<FragmentActivity>? = null

    fun setActivity(activity: FragmentActivity) {
        activityRef = WeakReference(activity)
    }

    fun getActivity(): FragmentActivity? = activityRef?.get()
}

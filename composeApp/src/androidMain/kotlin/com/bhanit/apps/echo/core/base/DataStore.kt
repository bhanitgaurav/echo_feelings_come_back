package com.bhanit.apps.echo.core.base

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

internal const val dataStoreFileName = "echo.preferences_pb"

class AndroidDataStoreFactory(private val context: Context) {
    fun create(): DataStore<Preferences> {
        return PreferenceDataStoreFactory.createWithPath(
            produceFile = { context.filesDir.resolve(dataStoreFileName).absolutePath.toPath() }
        )
    }
}

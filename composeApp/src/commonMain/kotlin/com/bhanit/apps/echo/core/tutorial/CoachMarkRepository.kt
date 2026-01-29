package com.bhanit.apps.echo.core.tutorial

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface CoachMarkRepository {
    fun isTutorialSeen(tutorialId: String): Flow<Boolean>
    suspend fun markTutorialSeen(tutorialId: String)
}

class CoachMarkRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : CoachMarkRepository {

    private fun getTutorialKey(id: String) = booleanPreferencesKey("tutorial_seen_$id")

    override fun isTutorialSeen(tutorialId: String): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[getTutorialKey(tutorialId)] ?: false
        }
    }

    override suspend fun markTutorialSeen(tutorialId: String) {
        dataStore.edit { preferences ->
            preferences[getTutorialKey(tutorialId)] = true
        }
    }
}

val LocalCoachMarkRepository = compositionLocalOf<CoachMarkRepository> {
    error("No CoachMarkRepository provided")
}

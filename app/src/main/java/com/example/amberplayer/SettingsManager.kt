package com.example.amberplayer // Make sure this matches your actual package name!

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// This creates the actual file on your phone's storage
private val Context.dataStore by preferencesDataStore(name = "amber_player_settings")

class SettingsManager(context: Context) {
    private val dataStore = context.dataStore

    // 1. Define the "Keys" (The names of the variables we want to save)
    companion object {
        val LAST_VOLUME = floatPreferencesKey("last_volume")
        val LAST_SONG_URI = stringPreferencesKey("last_song_uri")
    }

    // 2. Create "Flows" to read the data automatically
    // If there is no saved volume, it defaults to 0.5f (50%)
    val volumeFlow: Flow<Float> = dataStore.data.map { preferences ->
        preferences[LAST_VOLUME] ?: 0.5f
    }

    val songUriFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[LAST_SONG_URI]
    }

    // 3. Create suspend functions to write new data
    suspend fun saveVolume(volume: Float) {
        dataStore.edit { preferences ->
            preferences[LAST_VOLUME] = volume
        }
    }

    suspend fun saveSongUri(uri: String) {
        dataStore.edit { preferences ->
            preferences[LAST_SONG_URI] = uri
        }
    }
}
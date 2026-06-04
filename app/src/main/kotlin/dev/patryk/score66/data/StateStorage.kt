package dev.patryk.score66.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "score_keeper")

private val APP_STATE_KEY = stringPreferencesKey("app_state")

private val json = Json { ignoreUnknownKeys = true }

interface StateStorage {
    suspend fun load(): AppState?
    suspend fun save(state: AppState)
}

object NoOpStorage : StateStorage {
    override suspend fun load(): AppState? = null
    override suspend fun save(state: AppState) {}
}

class DataStoreStorage(private val dataStore: DataStore<Preferences>) : StateStorage {
    override suspend fun load(): AppState? = try {
        dataStore.data.first()[APP_STATE_KEY]?.let { json.decodeFromString(it) }
    } catch (_: Exception) {
        null
    }

    override suspend fun save(state: AppState) {
        try {
            dataStore.edit { prefs -> prefs[APP_STATE_KEY] = json.encodeToString(state) }
        } catch (_: Exception) {}
    }
}

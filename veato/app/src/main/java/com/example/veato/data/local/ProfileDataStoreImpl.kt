package com.example.veato.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.veato.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Extension to create DataStore instance
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_profile")

/**
 * DataStore implementation of ProfileLocalDataSource
 */
class ProfileDataStoreImpl(private val context: Context) : ProfileLocalDataSource {

    companion object {
        private fun profileKey(userId: String) = stringPreferencesKey("profile_$userId")
    }

    override suspend fun save(profile: UserProfile) {
        context.dataStore.edit { preferences ->
            preferences[profileKey(profile.userId)] = profile.toJson()
        }
    }

    override suspend fun get(userId: String): UserProfile? {
        return context.dataStore.data
            .map { preferences ->
                preferences[profileKey(userId)]?.let { json ->
                    try {
                        UserProfile.fromJson(json)
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            .first()
    }

    override fun getFlow(userId: String): Flow<UserProfile?> {
        return context.dataStore.data.map { preferences ->
            preferences[profileKey(userId)]?.let { json ->
                try {
                    UserProfile.fromJson(json)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    override suspend fun update(profile: UserProfile) {
        save(profile.withUpdatedTimestamp())
    }

    override suspend fun delete(userId: String) {
        context.dataStore.edit { preferences ->
            preferences.remove(profileKey(userId))
        }
    }

    override suspend fun exists(userId: String): Boolean {
        return get(userId) != null
    }
}

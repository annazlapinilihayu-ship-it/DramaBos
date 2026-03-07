package com.sonzaix.shortxrama.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.*

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DramaDataStore(private val context: Context) {
    private val historykey = stringPreferencesKey("watch_history_list_v7")
    private val favoritesKey = stringPreferencesKey("favorite_list_v1")
    private val gson = Gson()

    suspend fun addToHistory(item: LastWatched) {
        context.dataStore.edit { prefs ->
            val jsonString = prefs[historykey] ?: "[]"
            val type = object : TypeToken<MutableList<LastWatched>>() {}.type
            val list: MutableList<LastWatched> = try {
                gson.fromJson(jsonString, type) ?: mutableListOf()
            } catch (e: Exception) { Log.w("DramaDataStore", "Failed to parse history", e); mutableListOf() }

            val existingIndex = list.indexOfFirst { it.bookId == item.bookId }
            var itemToSave = item

            if (existingIndex != -1) {
                val oldItem = list[existingIndex]
                if (!oldItem.cover.isNullOrEmpty()) {
                    itemToSave = itemToSave.copy(cover = oldItem.cover)
                }
                list.removeAt(existingIndex)
            }

            list.add(0, itemToSave)

            if (list.size > 100) {
                list.subList(100, list.size).clear()
            }
            prefs[historykey] = gson.toJson(list)
        }
    }

    suspend fun removeHistoryItems(idsToRemove: List<String>) {
        context.dataStore.edit { prefs ->
            val jsonString = prefs[historykey] ?: "[]"
            val type = object : TypeToken<MutableList<LastWatched>>() {}.type
            val list: MutableList<LastWatched> = try {
                gson.fromJson(jsonString, type) ?: mutableListOf()
            } catch (e: Exception) { Log.w("DramaDataStore", "Failed to parse history for removal", e); mutableListOf() }

            list.removeAll { it.bookId in idsToRemove }
            prefs[historykey] = gson.toJson(list)
        }
    }

    val historyListFlow: Flow<List<LastWatched>> = context.dataStore.data.map { prefs ->
        val jsonString = prefs[historykey] ?: "[]"
        val type = object : TypeToken<List<LastWatched>>() {}.type
        try {
            gson.fromJson(jsonString, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun toggleFavorite(item: FavoriteDrama) {
        context.dataStore.edit { prefs ->
            val jsonString = prefs[favoritesKey] ?: "[]"
            val type = object : TypeToken<MutableList<FavoriteDrama>>() {}.type
            val list: MutableList<FavoriteDrama> = try {
                gson.fromJson(jsonString, type) ?: mutableListOf()
            } catch (e: Exception) { Log.w("DramaDataStore", "Failed to parse favorites", e); mutableListOf() }

            val existingIndex = list.indexOfFirst { it.bookId == item.bookId }

            if (existingIndex != -1) {
                list.removeAt(existingIndex)
            } else {
                list.add(0, item)
            }

            prefs[favoritesKey] = gson.toJson(list)
        }
    }

    suspend fun removeFavoriteItems(idsToRemove: List<String>) {
        context.dataStore.edit { prefs ->
            val jsonString = prefs[favoritesKey] ?: "[]"
            val type = object : TypeToken<MutableList<FavoriteDrama>>() {}.type
            val list: MutableList<FavoriteDrama> = try {
                gson.fromJson(jsonString, type) ?: mutableListOf()
            } catch (e: Exception) { Log.w("DramaDataStore", "Failed to parse favorites for removal", e); mutableListOf() }

            list.removeAll { it.bookId in idsToRemove }
            prefs[favoritesKey] = gson.toJson(list)
        }
    }

    val favoritesListFlow: Flow<List<FavoriteDrama>> = context.dataStore.data.map { prefs ->
        val jsonString = prefs[favoritesKey] ?: "[]"
        val type = object : TypeToken<List<FavoriteDrama>>() {}.type
        try {
            gson.fromJson(jsonString, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun exportBackupJson(): String {
        val history = historyListFlow.first()
        val favorites = favoritesListFlow.first()
        return gson.toJson(BackupPayload(history = history, favorites = favorites))
    }

    suspend fun importBackupJson(json: String): Boolean {
        val payload = try {
            gson.fromJson(json, BackupPayload::class.java)
        } catch (_: Exception) {
            null
        } ?: return false

        context.dataStore.edit { prefs ->
            prefs[historykey] = gson.toJson(payload.history)
            prefs[favoritesKey] = gson.toJson(payload.favorites)
        }
        return true
    }
}

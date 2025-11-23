package com.example.batterywallpaper.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class WallpaperSettings(val animationLevel: Float, val batterySize: Float, val batteryColor: Int, val backgroundColor: Int)

class WallpaperSettingsRepository(private val context: Context) {

    val wallpaperSettings: Flow<WallpaperSettings> = context.dataStore.data
        .map {
            WallpaperSettings(
                animationLevel = it[KEY_ANIMATION_LEVEL] ?: 1f,
                batterySize = it[KEY_BATTERY_SIZE] ?: 1f,
                batteryColor = it[KEY_BATTERY_COLOR] ?: 0xFFFFFFFF.toInt(),
                backgroundColor = it[KEY_BACKGROUND_COLOR] ?: 0xFF101010.toInt(),
            )
        }

    suspend fun setAnimationLevel(level: Float) {
        context.dataStore.edit { it[KEY_ANIMATION_LEVEL] = level }
    }

    suspend fun setBatterySize(size: Float) {
        context.dataStore.edit { it[KEY_BATTERY_SIZE] = size }
    }

    suspend fun setBatteryColor(color: Int) {
        context.dataStore.edit { it[KEY_BATTERY_COLOR] = color }
    }

    suspend fun setBackgroundColor(color: Int) {
        context.dataStore.edit { it[KEY_BACKGROUND_COLOR] = color }
    }

    companion object {
        private val KEY_ANIMATION_LEVEL = floatPreferencesKey("animation_level")
        private val KEY_BATTERY_SIZE = floatPreferencesKey("battery_size")
        private val KEY_BATTERY_COLOR = intPreferencesKey("battery_color")
        private val KEY_BACKGROUND_COLOR = intPreferencesKey("background_color")
    }
}

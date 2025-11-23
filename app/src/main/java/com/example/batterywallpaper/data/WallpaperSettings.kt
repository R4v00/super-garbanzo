package com.example.batterywallpaper.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class WallpaperSettings(
    val animationLevel: Float,
    val batteryWidth: Float,
    val batteryHeight: Float,
    val batteryColor: Int,
    val backgroundColor: Int,
    val textColor: Int,
    val edgesColor: Int
)

class WallpaperSettingsRepository(private val context: Context) {

    val wallpaperSettings: Flow<WallpaperSettings> = context.dataStore.data
        .map {
            WallpaperSettings(
                animationLevel = it[KEY_ANIMATION_LEVEL] ?: 1f,
                batteryWidth = it[KEY_BATTERY_WIDTH] ?: 0.6f,
                batteryHeight = it[KEY_BATTERY_HEIGHT] ?: 0.3f,
                batteryColor = it[KEY_BATTERY_COLOR] ?: Color.Green.hashCode(),
                backgroundColor = it[KEY_BACKGROUND_COLOR] ?: Color.Black.hashCode(),
                textColor = it[KEY_TEXT_COLOR] ?: Color.White.hashCode(),
                edgesColor = it[KEY_EDGES_COLOR] ?: Color.White.hashCode()
            )
        }

    suspend fun setAnimationLevel(level: Float) {
        context.dataStore.edit { it[KEY_ANIMATION_LEVEL] = level }
    }

    suspend fun setBatteryWidth(width: Float) {
        context.dataStore.edit { it[KEY_BATTERY_WIDTH] = width }
    }

    suspend fun setBatteryHeight(height: Float) {
        context.dataStore.edit { it[KEY_BATTERY_HEIGHT] = height }
    }

    suspend fun setBatteryColor(color: Int) {
        context.dataStore.edit { it[KEY_BATTERY_COLOR] = color }
    }

    suspend fun setBackgroundColor(color: Int) {
        context.dataStore.edit { it[KEY_BACKGROUND_COLOR] = color }
    }

    suspend fun setTextColor(color: Int) {
        context.dataStore.edit { it[KEY_TEXT_COLOR] = color }
    }

    suspend fun setEdgesColor(color: Int) {
        context.dataStore.edit { it[KEY_EDGES_COLOR] = color }
    }

    companion object {
        private val KEY_ANIMATION_LEVEL = floatPreferencesKey("animation_level")
        private val KEY_BATTERY_WIDTH = floatPreferencesKey("battery_width")
        private val KEY_BATTERY_HEIGHT = floatPreferencesKey("battery_height")
        private val KEY_BATTERY_COLOR = intPreferencesKey("battery_color")
        private val KEY_BACKGROUND_COLOR = intPreferencesKey("background_color")
        private val KEY_TEXT_COLOR = intPreferencesKey("text_color")
        private val KEY_EDGES_COLOR = intPreferencesKey("edges_color")
    }
}

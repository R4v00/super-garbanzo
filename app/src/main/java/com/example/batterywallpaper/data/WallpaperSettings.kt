package com.example.batterywallpaper.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class WallpaperSettings(
    val batteryStyle: BatteryStyle,
    val gaugeAnimationLevel: Float,
    val edgeAnimationLevel: Float,
    val batteryWidth: Float,
    val batteryHeight: Float,
    val batteryColor: Int,
    val backgroundColor: Int,
    val textColor: Int,
    val edgesColor: Int,
    val textSize: Float
)

class WallpaperSettingsRepository(private val context: Context) {

    val wallpaperSettings: Flow<WallpaperSettings> = context.dataStore.data
        .map {
            WallpaperSettings(
                batteryStyle = BatteryStyle.fromId(it[KEY_BATTERY_STYLE]),
                gaugeAnimationLevel = it[KEY_GAUGE_ANIMATION_LEVEL] ?: 1f,
                edgeAnimationLevel = it[KEY_EDGE_ANIMATION_LEVEL] ?: 1f,
                batteryWidth = it[KEY_BATTERY_WIDTH] ?: 0.6f,
                batteryHeight = it[KEY_BATTERY_HEIGHT] ?: 0.3f,
                batteryColor = it[KEY_BATTERY_COLOR] ?: Color.White.toArgb(),
                backgroundColor = it[KEY_BACKGROUND_COLOR] ?: Color(0xFF222222).toArgb(),
                textColor = it[KEY_TEXT_COLOR] ?: Color.White.toArgb(),
                edgesColor = it[KEY_EDGES_COLOR] ?: Color.White.toArgb(),
                textSize = it[KEY_TEXT_SIZE] ?: 0.5f
            )
        }

    suspend fun setBatteryStyle(style: BatteryStyle) {
        context.dataStore.edit { it[KEY_BATTERY_STYLE] = style.id }
    }

    suspend fun setGaugeAnimationLevel(level: Float) {
        context.dataStore.edit { it[KEY_GAUGE_ANIMATION_LEVEL] = level }
    }

    suspend fun setEdgeAnimationLevel(level: Float) {
        context.dataStore.edit { it[KEY_EDGE_ANIMATION_LEVEL] = level }
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

    suspend fun setTextSize(size: Float) {
        context.dataStore.edit { it[KEY_TEXT_SIZE] = size }
    }

    companion object {
        private val KEY_BATTERY_STYLE = stringPreferencesKey("battery_style")
        private val KEY_GAUGE_ANIMATION_LEVEL = floatPreferencesKey("gauge_animation_level")
        private val KEY_EDGE_ANIMATION_LEVEL = floatPreferencesKey("edge_animation_level")
        private val KEY_BATTERY_WIDTH = floatPreferencesKey("battery_width")
        private val KEY_BATTERY_HEIGHT = floatPreferencesKey("battery_height")
        private val KEY_BATTERY_COLOR = intPreferencesKey("battery_color")
        private val KEY_BACKGROUND_COLOR = intPreferencesKey("background_color")
        private val KEY_TEXT_COLOR = intPreferencesKey("text_color")
        private val KEY_EDGES_COLOR = intPreferencesKey("edges_color")
        private val KEY_TEXT_SIZE = floatPreferencesKey("text_size")
    }
}
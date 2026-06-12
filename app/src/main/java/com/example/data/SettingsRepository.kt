package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    companion object {
        val GAMING_MODE_ENABLED = booleanPreferencesKey("gaming_mode")
        val DND_ENABLED = booleanPreferencesKey("dnd_enabled")
        val BRIGHTNESS_FIX = booleanPreferencesKey("brightness_fix")
        val AUTO_BOOST_ENABLED = booleanPreferencesKey("auto_boost")
        val TEMP_THRESHOLD = intPreferencesKey("temp_threshold")
        val OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
        val OVERLAY_TRANSPARENCY = floatPreferencesKey("overlay_transparency")
        val OVERLAY_SIZE = floatPreferencesKey("overlay_size")
        val GAMING_BRIGHTNESS = intPreferencesKey("gaming_brightness")
        val DISABLE_ADAPTIVE_BRIGHTNESS = booleanPreferencesKey("disable_adaptive_brightness")
        val GAMING_DISPLAY_MODE = booleanPreferencesKey("gaming_display_mode")
    }

    val gamingModeEnabled: Flow<Boolean> = context.dataStore.data.map { it[GAMING_MODE_ENABLED] ?: false }
    val dndEnabled: Flow<Boolean> = context.dataStore.data.map { it[DND_ENABLED] ?: false }
    val brightnessFix: Flow<Boolean> = context.dataStore.data.map { it[BRIGHTNESS_FIX] ?: false }
    val autoBoostEnabled: Flow<Boolean> = context.dataStore.data.map { it[AUTO_BOOST_ENABLED] ?: false }
    val tempThreshold: Flow<Int> = context.dataStore.data.map { it[TEMP_THRESHOLD] ?: 40 }
    val overlayEnabled: Flow<Boolean> = context.dataStore.data.map { it[OVERLAY_ENABLED] ?: false }
    val overlayTransparency: Flow<Float> = context.dataStore.data.map { it[OVERLAY_TRANSPARENCY] ?: 0.8f }
    val overlaySize: Flow<Float> = context.dataStore.data.map { it[OVERLAY_SIZE] ?: 1.0f }
    val gamingBrightness: Flow<Int> = context.dataStore.data.map { it[GAMING_BRIGHTNESS] ?: 128 }
    val disableAdaptiveBrightness: Flow<Boolean> = context.dataStore.data.map { it[DISABLE_ADAPTIVE_BRIGHTNESS] ?: false }
    val gamingDisplayMode: Flow<Boolean> = context.dataStore.data.map { it[GAMING_DISPLAY_MODE] ?: false }

    suspend fun setGamingMode(enabled: Boolean) = context.dataStore.edit { it[GAMING_MODE_ENABLED] = enabled }
    suspend fun setDnd(enabled: Boolean) = context.dataStore.edit { it[DND_ENABLED] = enabled }
    suspend fun setBrightnessFix(enabled: Boolean) = context.dataStore.edit { it[BRIGHTNESS_FIX] = enabled }
    suspend fun setAutoBoost(enabled: Boolean) = context.dataStore.edit { it[AUTO_BOOST_ENABLED] = enabled }
    suspend fun setTempThreshold(temp: Int) = context.dataStore.edit { it[TEMP_THRESHOLD] = temp }
    suspend fun setOverlayEnabled(enabled: Boolean) = context.dataStore.edit { it[OVERLAY_ENABLED] = enabled }
    suspend fun setOverlayTransparency(alpha: Float) = context.dataStore.edit { it[OVERLAY_TRANSPARENCY] = alpha }
    suspend fun setOverlaySize(scale: Float) = context.dataStore.edit { it[OVERLAY_SIZE] = scale }
    suspend fun setGamingBrightness(brightness: Int) = context.dataStore.edit { it[GAMING_BRIGHTNESS] = brightness }
    suspend fun setDisableAdaptiveBrightness(enabled: Boolean) = context.dataStore.edit { it[DISABLE_ADAPTIVE_BRIGHTNESS] = enabled }
    suspend fun setGamingDisplayMode(enabled: Boolean) = context.dataStore.edit { it[GAMING_DISPLAY_MODE] = enabled }
}

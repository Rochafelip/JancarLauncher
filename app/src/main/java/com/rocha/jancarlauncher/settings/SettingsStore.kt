package com.rocha.jancarlauncher.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Preferências do launcher (tema, brilho, contador de boot). Recebe o DataStore para ser testável. */
class SettingsStore(private val ds: DataStore<Preferences>) {
    private val darkKey = booleanPreferencesKey("dark_theme")
    private val userBrightnessKey = intPreferencesKey("user_brightness")
    private val bootCountKey = intPreferencesKey("last_boot_count")

    val dark: Flow<Boolean> = ds.data.map { it[darkKey] ?: true }
    /** Brilho (%) escolhido pelo usuário no launcher; null = nunca escolheu (não mexer no brilho da central). */
    val userBrightness: Flow<Int?> = ds.data.map { it[userBrightnessKey] }
    val lastBootCount: Flow<Int?> = ds.data.map { it[bootCountKey] }

    suspend fun setDark(value: Boolean) { ds.edit { it[darkKey] = value } }
    suspend fun setUserBrightness(value: Int) { ds.edit { it[userBrightnessKey] = value } }
    suspend fun setLastBootCount(value: Int) { ds.edit { it[bootCountKey] = value } }
}

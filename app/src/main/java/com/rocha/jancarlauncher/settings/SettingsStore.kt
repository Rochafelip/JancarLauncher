package com.rocha.jancarlauncher.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Preferências do launcher (tema, brilho, contador de boot). Recebe o DataStore para ser testável. */
class SettingsStore(private val ds: DataStore<Preferences>) {
    private val darkKey = booleanPreferencesKey("dark_theme")
    private val userBrightnessKey = intPreferencesKey("user_brightness")
    private val bootCountKey = intPreferencesKey("last_boot_count")
    private val nightAppliedKey = booleanPreferencesKey("night_mode_applied")
    private val preNightDarkKey = booleanPreferencesKey("pre_night_dark")
    private val preNightBrightnessKey = intPreferencesKey("pre_night_brightness")

    private val driveModeKey = stringPreferencesKey("drive_mode")
    private val fuelBlendKey = stringPreferencesKey("fuel_blend")
    private val homeLayoutKey = stringPreferencesKey("home_layout")

    /** Tema (arranjo) da Home (nome do `HomeLayout`); null = nunca escolheu (A, o de sempre). */
    val homeLayout: Flow<String?> = ds.data.map { it[homeLayoutKey] }
    suspend fun setHomeLayout(name: String) { ds.edit { it[homeLayoutKey] = name } }
    private val tripLitersKey = doublePreferencesKey("trip_liters")
    private val tripKmKey = doublePreferencesKey("trip_km")

    /** Mistura no tanque escolhida no Painel (nome do `FuelModel.Blend`); null = nunca escolheu (gasolina). */
    val fuelBlend: Flow<String?> = ds.data.map { it[fuelBlendKey] }
    suspend fun setFuelBlend(name: String) { ds.edit { it[fuelBlendKey] = name } }

    /** Média da viagem (OBD): litros e km somados desde o último "zerar". */
    val trip: Flow<Pair<Double, Double>> = ds.data.map { (it[tripLitersKey] ?: 0.0) to (it[tripKmKey] ?: 0.0) }
    suspend fun setTrip(liters: Double, km: Double) { ds.edit { it[tripLitersKey] = liters; it[tripKmKey] = km } }

    val dark: Flow<Boolean> = ds.data.map { it[darkKey] ?: true }
    /** Modo do aviso de troca do Painel (nome do `DashModel.DriveMode`); null = nunca escolheu. */
    val driveMode: Flow<String?> = ds.data.map { it[driveModeKey] }
    suspend fun setDriveMode(name: String) { ds.edit { it[driveModeKey] = name } }
    /** Brilho (%) escolhido pelo usuário no launcher; null = nunca escolheu (não mexer no brilho da central). */
    val userBrightness: Flow<Int?> = ds.data.map { it[userBrightnessKey] }
    val lastBootCount: Flow<Int?> = ds.data.map { it[bootCountKey] }
    /** Se a automação de modo noturno está com o estado noturno aplicado agora. */
    val nightModeApplied: Flow<Boolean> = ds.data.map { it[nightAppliedKey] ?: false }
    /** Tema/brilho salvos de antes da última entrada na janela noturna, para restaurar na saída. */
    val preNightDark: Flow<Boolean> = ds.data.map { it[preNightDarkKey] ?: true }
    val preNightBrightness: Flow<Int?> = ds.data.map { it[preNightBrightnessKey] }

    suspend fun setDark(value: Boolean) { ds.edit { it[darkKey] = value } }
    suspend fun setUserBrightness(value: Int) { ds.edit { it[userBrightnessKey] = value } }
    suspend fun setLastBootCount(value: Int) { ds.edit { it[bootCountKey] = value } }

    suspend fun applyNightMode(previousDark: Boolean, previousBrightness: Int?) {
        ds.edit {
            it[nightAppliedKey] = true
            it[preNightDarkKey] = previousDark
            it[darkKey] = true
            // Só mexe no brilho se o usuário já tinha escolhido um — nunca aplica brilho não pedido.
            if (previousBrightness != null) {
                it[preNightBrightnessKey] = previousBrightness
                it[userBrightnessKey] = NightModePolicy.NIGHT_BRIGHTNESS
            }
        }
    }

    suspend fun restoreDayMode(dark: Boolean, brightness: Int?) {
        ds.edit {
            it[nightAppliedKey] = false
            it[darkKey] = dark
            if (brightness != null) it[userBrightnessKey] = brightness
        }
    }
}

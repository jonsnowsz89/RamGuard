package com.ramguard.app.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "ramguard_settings")

/** الأوضاع الثلاثة / The three power profiles */
enum class PowerMode { NORMAL, ECO, ULTRA }

class SettingsRepository(private val context: Context) {

    private object Keys {
        val WHITELIST = stringSetPreferencesKey("whitelist")
        val MODE = stringPreferencesKey("mode")
        val AUTO_CLEAN = booleanPreferencesKey("auto_clean_on_screen_off")
        val UNLOCK_UNTIL = longPreferencesKey("unlock_until")
        val SAVED_DND = intPreferencesKey("saved_dnd_filter")
        val BLOCK_DATA = booleanPreferencesKey("block_background_data")
    }

    val whitelist: Flow<Set<String>> = context.dataStore.data.map { it[Keys.WHITELIST] ?: emptySet() }
    val mode: Flow<PowerMode> = context.dataStore.data.map {
        runCatching { PowerMode.valueOf(it[Keys.MODE] ?: "NORMAL") }.getOrDefault(PowerMode.NORMAL)
    }
    val autoClean: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_CLEAN] ?: true }
    /** Epoch millis until which restrictions are paused (manual communication window). */
    val unlockUntil: Flow<Long> = context.dataStore.data.map { it[Keys.UNLOCK_UNTIL] ?: 0L }
    /** The user's own Do-Not-Disturb filter, saved before we override it. 0 = nothing saved. */
    val savedDnd: Flow<Int> = context.dataStore.data.map { it[Keys.SAVED_DND] ?: 0 }

    /** Opt-in: block internet of non-whitelisted apps through the local VPN while in ULTRA mode. */
    val blockData: Flow<Boolean> = context.dataStore.data.map { it[Keys.BLOCK_DATA] ?: false }
    suspend fun setBlockData(v: Boolean) { context.dataStore.edit { it[Keys.BLOCK_DATA] = v } }

    suspend fun toggleWhitelist(pkg: String) {
        context.dataStore.edit {
            val cur = it[Keys.WHITELIST] ?: emptySet()
            it[Keys.WHITELIST] = if (pkg in cur) cur - pkg else cur + pkg
        }
    }
    suspend fun setMode(m: PowerMode) { context.dataStore.edit { it[Keys.MODE] = m.name } }
    suspend fun setAutoClean(v: Boolean) { context.dataStore.edit { it[Keys.AUTO_CLEAN] = v } }
    suspend fun unlockFor(minutes: Int) {
        context.dataStore.edit { it[Keys.UNLOCK_UNTIL] = System.currentTimeMillis() + minutes * 60_000L }
    }
    suspend fun clearUnlock() { context.dataStore.edit { it[Keys.UNLOCK_UNTIL] = 0L } }
    suspend fun isUnlocked(): Boolean = System.currentTimeMillis() < unlockUntil.first()
    suspend fun setSavedDnd(v: Int) { context.dataStore.edit { it[Keys.SAVED_DND] = v } }
}

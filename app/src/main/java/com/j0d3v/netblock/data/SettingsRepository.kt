package com.j0d3v.netblock.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class ThemeSettings(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val amoled: Boolean = false,
    val dynamicColor: Boolean = true,
)

/** UI-only preferences: theme and the one-time onboarding flag. */
class SettingsRepository(private val store: DataStore<Preferences>) {

    constructor(context: Context) : this(context.applicationContext.netblockDataStore)

    private val onboardedKey = booleanPreferencesKey("onboarded")
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val amoledKey = booleanPreferencesKey("amoled")
    private val dynamicColorKey = booleanPreferencesKey("dynamic_color")

    val themeSettings: Flow<ThemeSettings> =
        store.data.map { p ->
            ThemeSettings(
                mode = p[themeModeKey]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                    ?: ThemeMode.SYSTEM,
                amoled = p[amoledKey] ?: false,
                dynamicColor = p[dynamicColorKey] ?: true,
            )
        }

    suspend fun setThemeMode(mode: ThemeMode) = store.edit { it[themeModeKey] = mode.name }
    suspend fun setAmoled(enabled: Boolean) = store.edit { it[amoledKey] = enabled }
    suspend fun setDynamicColor(enabled: Boolean) = store.edit { it[dynamicColorKey] = enabled }

    val onboarded: Flow<Boolean> =
        store.data.map { it[onboardedKey] ?: false }

    suspend fun setOnboarded() {
        store.edit { it[onboardedKey] = true }
    }
}

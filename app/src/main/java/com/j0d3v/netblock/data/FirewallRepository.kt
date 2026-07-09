package com.j0d3v.netblock.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Firewall state: which packages are blocked and whether the VPN is on. */
class FirewallRepository(private val store: DataStore<Preferences>) {

    constructor(context: Context) : this(context.applicationContext.netblockDataStore)

    private val blockedKey = stringSetPreferencesKey("blocked_packages")
    private val vpnKey = booleanPreferencesKey("vpn_enabled")

    val blockedPackages: Flow<Set<String>> =
        store.data.map { it[blockedKey] ?: emptySet() }

    val vpnEnabled: Flow<Boolean> =
        store.data.map { it[vpnKey] ?: false }

    suspend fun setVpnEnabled(enabled: Boolean) {
        store.edit { it[vpnKey] = enabled }
    }

    suspend fun toggle(packageName: String) {
        store.edit { prefs ->
            val current = prefs[blockedKey] ?: emptySet()
            prefs[blockedKey] =
                if (packageName in current) current - packageName
                else current + packageName
        }
    }

    suspend fun setBlocked(packages: Collection<String>, blocked: Boolean) {
        store.edit { prefs ->
            val current = prefs[blockedKey] ?: emptySet()
            prefs[blockedKey] = if (blocked) current + packages else current - packages.toSet()
        }
    }
}

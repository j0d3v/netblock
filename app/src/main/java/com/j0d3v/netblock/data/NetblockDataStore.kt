package com.j0d3v.netblock.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

internal val Context.netblockDataStore by preferencesDataStore(name = "blocked_apps")

package com.j0d3v.netblock.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.j0d3v.netblock.data.SettingsRepository
import com.j0d3v.netblock.data.ThemeMode
import com.j0d3v.netblock.data.ThemeSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SettingsRepository(app)

    val settings: StateFlow<ThemeSettings> =
        repo.themeSettings.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeSettings(),
        )

    fun setMode(mode: ThemeMode) = viewModelScope.launch { repo.setThemeMode(mode) }
    fun setAmoled(enabled: Boolean) = viewModelScope.launch { repo.setAmoled(enabled) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { repo.setDynamicColor(enabled) }
}

package com.j0d3v.netblock.ui

import android.app.Application
import android.content.Intent
import android.net.VpnService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.j0d3v.netblock.data.FirewallRepository
import com.j0d3v.netblock.data.InstalledApp
import com.j0d3v.netblock.data.loadInstalledApps
import com.j0d3v.netblock.vpn.BlockerVpnService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppUi(
    val packageName: String,
    val label: String,
    val isBlocked: Boolean,
)

sealed interface AppListState {
    data object Loading : AppListState
    data class Ready(val apps: List<AppUi>) : AppListState
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = FirewallRepository(app)

    private val installed = MutableStateFlow<List<InstalledApp>?>(null)

    init {
        viewModelScope.launch { installed.value = loadInstalledApps(app) }
    }

    val vpnRunning: StateFlow<Boolean> =
        repo.vpnEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val state: StateFlow<AppListState> =
        combine(installed, repo.blockedPackages) { apps, blocked ->
            if (apps == null) {
                AppListState.Loading
            } else {
                AppListState.Ready(
                    apps.map { // already sorted by label at load time
                        AppUi(it.packageName, it.label, it.packageName in blocked)
                    },
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppListState.Loading)

    fun toggle(packageName: String) {
        viewModelScope.launch { repo.toggle(packageName) }
    }

    fun setBlocked(packages: List<String>, blocked: Boolean) {
        viewModelScope.launch { repo.setBlocked(packages, blocked) }
    }

    fun vpnConsentIntent(): Intent? = VpnService.prepare(getApplication())

    fun startVpn() {
        val ctx = getApplication<Application>()
        viewModelScope.launch { repo.setVpnEnabled(true) }
        ctx.startForegroundService(
            Intent(ctx, BlockerVpnService::class.java).setAction(BlockerVpnService.ACTION_START),
        )
    }

    fun stopVpn() {
        val ctx = getApplication<Application>()
        viewModelScope.launch { repo.setVpnEnabled(false) }
        ctx.startService(
            Intent(ctx, BlockerVpnService::class.java).setAction(BlockerVpnService.ACTION_STOP),
        )
    }
}

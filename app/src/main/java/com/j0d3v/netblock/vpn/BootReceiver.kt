package com.j0d3v.netblock.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import com.j0d3v.netblock.data.FirewallRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        // DataStore's first read hits disk; goAsync moves it off the main thread
        // instead of blocking onReceive (the receiver stays alive until finish()).
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = FirewallRepository(context)
                if (!repo.vpnEnabled.first()) return@launch

                // VpnService.prepare() returns null only while consent is still granted.
                // If the OS dropped consent across the reboot, establish() would silently
                // fail and the icon would lie — so clear the flag instead of pretending we're on.
                if (VpnService.prepare(context) != null) {
                    repo.setVpnEnabled(false)
                    return@launch
                }

                // BOOT_COMPLETED is one of the few contexts allowed to start a foreground service.
                context.startForegroundService(
                    Intent(context, BlockerVpnService::class.java)
                        .setAction(BlockerVpnService.ACTION_START),
                )
            } finally {
                pending.finish()
            }
        }
    }
}

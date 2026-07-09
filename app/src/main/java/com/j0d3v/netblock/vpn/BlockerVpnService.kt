package com.j0d3v.netblock.vpn

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager.NameNotFoundException
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import com.j0d3v.netblock.MainActivity
import com.j0d3v.netblock.R
import com.j0d3v.netblock.data.FirewallRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

/**
 * No-root per-app firewall using the VpnService allowlist trick.
 *
 * addAllowedApplication(pkg) routes only the listed apps through the tunnel;
 * we then drop every packet, so blocked apps get no network and everything else
 * bypasses the VPN in the kernel at zero cost.
 *
 * Careful with the empty set: an empty allowlist means "all apps", so instead of
 * establishing a tunnel that would blackhole the whole device, we tear it down.
 */
@SuppressLint("VpnServicePolicy")
class BlockerVpnService : VpnService() {

    companion object {
        const val ACTION_START = "com.j0d3v.netblock.START"
        const val ACTION_STOP = "com.j0d3v.netblock.STOP"

        private const val CHANNEL_ID = "netblock_vpn"
        private const val NOTIF_ID = 1
    }

    private val repo by lazy { FirewallRepository(this) }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var watchJob: Job? = null
    private var tunnel: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopVpn()
            return START_NOT_STICKY
        }
        ensureRunning()
        return START_STICKY
    }

    override fun onRevoke() {
        serviceScope.launch { repo.setVpnEnabled(false) }
        stopVpn()
    }

    override fun onDestroy() {
        super.onDestroy()
        watchJob?.cancel()
        watchJob = null
        closeTunnel()
        serviceScope.cancel()
    }

    @OptIn(FlowPreview::class)
    private fun ensureRunning() {
        startForegroundCompat() // required within 5s of startForegroundService on every API >= 26
        if (watchJob != null) return

        watchJob = serviceScope.launch {
            repo.blockedPackages
                .debounce(300.milliseconds)
                .distinctUntilChanged()
                .collect { establish(it) }
        }
    }

    private fun stopVpn() {
        watchJob?.cancel()
        watchJob = null
        closeTunnel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    @Synchronized
    private fun establish(blocked: Set<String>) {
        if (blocked.isEmpty()) { // see class doc
            closeTunnel()
            return
        }

        val builder = Builder()
            .setSession("Netblock")
            .setMtu(1500)
            // Dummy endpoints — every packet is dropped, so they're never reached.
            .addAddress("10.0.0.2", 32)
            .addAddress("fd00:1:1:1::2", 128)
            .addRoute("0.0.0.0", 0)
            .addRoute("::", 0)

        var added = 0
        for (pkg in blocked) {
            try {
                builder.addAllowedApplication(pkg)
                added++
            } catch (_: NameNotFoundException) {
                // App uninstalled since it was blocked — skip it.
            }
        }
        // If every blocked app is gone the allowlist is empty, which means "all
        // apps" — establishing now would blackhole the whole device. Bail instead.
        if (added == 0) {
            closeTunnel()
            return
        }

        // Establish the new interface BEFORE closing the old one: the kernel
        // replaces it atomically, so blocked apps never get a window of network.
        val fd = builder.establish()
        if (fd == null) {
            // establish() only returns null when VPN consent is gone. Reflect
            // reality so the UI icon can't claim we're on while nothing is routed.
            serviceScope.launch { repo.setVpnEnabled(false) }
            stopVpn()
            return
        }
        val old = tunnel
        tunnel = fd
        try {
            old?.close()
        } catch (_: IOException) {
        }
    }

    @Synchronized
    private fun closeTunnel() {
        try {
            tunnel?.close()
        } catch (_: IOException) {
        }
        tunnel = null
    }

    private fun startForegroundCompat() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW,
            ),
        )
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification: Notification =
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.notif_active_title))
                .setContentText(getString(R.string.notif_active_text))
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .build()

        // SPECIAL_USE type constant and its runtime enforcement are API 34+; below
        // that the plain overload satisfies the startForegroundService contract.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }
}

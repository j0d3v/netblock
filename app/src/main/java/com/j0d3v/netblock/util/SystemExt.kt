package com.j0d3v.netblock.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

fun Context.startActivitySafe(intent: Intent) {
    runCatching { startActivity(intent) } // some OEMs lack the target settings screen
}

fun Context.hasNotificationPermission(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

fun Context.isBatteryExempt(): Boolean =
    getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)

fun Context.isVpnConsented(): Boolean = VpnService.prepare(this) == null

fun Context.openBatterySettings() =
    startActivitySafe(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))

fun Context.openVpnSettings() =
    startActivitySafe(Intent(Settings.ACTION_VPN_SETTINGS))

fun Context.openAppNotificationSettings() =
    startActivitySafe(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
    )

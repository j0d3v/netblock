package com.j0d3v.netblock.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledApp(
    val packageName: String,
    val label: String,
)

// Icons are intentionally NOT loaded here — decoding every app's icon at startup
// is the app's heaviest cost. They're loaded lazily per visible row instead.
// Only apps that request INTERNET are listed; the rest can't be firewalled anyway.
@Suppress("DEPRECATION")
suspend fun loadInstalledApps(context: Context): List<InstalledApp> =
    withContext(Dispatchers.IO) {
        val pm = context.packageManager
        pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            .asSequence()
            .filter { it.packageName != context.packageName }
            .filter { it.requestedPermissions?.contains(Manifest.permission.INTERNET) == true }
            .mapNotNull { pkg ->
                pkg.applicationInfo?.let {
                    InstalledApp(pkg.packageName, pm.getApplicationLabel(it).toString())
                }
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

@file:Suppress("FunctionName")

package com.j0d3v.netblock.ui

import android.Manifest
import android.net.VpnService
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.j0d3v.netblock.R
import com.j0d3v.netblock.data.SettingsRepository
import com.j0d3v.netblock.util.hasNotificationPermission
import com.j0d3v.netblock.util.isBatteryExempt
import com.j0d3v.netblock.util.isVpnConsented
import com.j0d3v.netblock.util.openAppNotificationSettings
import com.j0d3v.netblock.util.openBatterySettings
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(repo: SettingsRepository, onFinish: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Checked once at start; VPN and notifications update from their launcher
    // callbacks, so no repeated polling. Battery opens system settings (no
    // result), so its checkmark reflects the state at launch.
    var notifGranted by remember { mutableStateOf(context.hasNotificationPermission()) }
    var notifDenied by remember { mutableStateOf(false) }
    var vpnGranted by remember { mutableStateOf(context.isVpnConsented()) }
    val batteryExempt = remember { context.isBatteryExempt() }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notifGranted = granted
        // After a denial the OS won't show the dialog again, so send the user to
        // app settings on the next tap instead of a button that silently no-ops.
        notifDenied = !granted
    }
    val vpnConsent = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { vpnGranted = context.isVpnConsented() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.onboarding_welcome), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.onboarding_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))

        SetupCard(
            icon = Icons.Filled.Lock,
            title = stringResource(R.string.perm_vpn_title),
            desc = stringResource(R.string.perm_vpn_desc),
            granted = vpnGranted,
            action = stringResource(R.string.action_grant),
        ) { VpnService.prepare(context)?.let(vpnConsent::launch) }

        SetupCard(
            icon = Icons.Filled.Notifications,
            title = stringResource(R.string.perm_notif_title),
            desc = stringResource(R.string.perm_notif_desc),
            granted = notifGranted,
            action = stringResource(R.string.action_grant),
        ) {
            when {
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> Unit
                notifDenied -> context.openAppNotificationSettings()
                else -> notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        SetupCard(
            icon = Icons.Filled.Settings,
            title = stringResource(R.string.perm_battery_title),
            desc = stringResource(R.string.perm_battery_desc),
            granted = batteryExempt,
            action = stringResource(R.string.action_open_settings),
        ) { context.openBatterySettings() }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { scope.launch { repo.setOnboarded(); onFinish() } },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.get_started)) }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SetupCard(
    icon: ImageVector,
    title: String,
    desc: String,
    granted: Boolean,
    action: String,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        if (granted) {
            Icon(
                Icons.Filled.Check,
                contentDescription = stringResource(R.string.cd_granted),
                tint = MaterialTheme.colorScheme.primary,
            )
        } else {
            OutlinedButton(onClick = onAction) { Text(action) }
        }
    }
}

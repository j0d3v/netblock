@file:Suppress("FunctionName")

package com.j0d3v.netblock.ui

import android.app.Activity
import android.util.LruCache
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.j0d3v.netblock.R
import com.j0d3v.netblock.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun NetblockApp(repo: SettingsRepository) {
    val onboarded by repo.onboarded.collectAsStateWithLifecycle(initialValue = null)

    // Wait until the flag is read so NavHost gets the right start destination the
    // first time (startDestination is only honored on initial composition).
    onboarded?.let { done ->
        val nav = rememberNavController()
        NavHost(navController = nav, startDestination = if (done) "main" else "onboarding") {
            composable("onboarding") {
                OnboardingScreen(repo = repo, onFinish = {
                    nav.navigate("main") { popUpTo("onboarding") { inclusive = true } }
                })
            }
            composable("main") { MainScreen(onOpenSettings = { nav.navigate("settings") }) }
            composable("settings") { SettingsScreen(onBack = { nav.popBackStack() }) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    onOpenSettings: () -> Unit,
    vm: MainViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val running by vm.vpnRunning.collectAsStateWithLifecycle()
    val toggle = rememberVpnToggle(vm, running)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                navigationIcon = {
                    IconButton(onClick = toggle) {
                        Icon(
                            imageVector = if (running) Icons.Filled.Lock else Icons.Outlined.Lock,
                            contentDescription = stringResource(
                                if (running) R.string.cd_vpn_on else R.string.cd_vpn_off,
                            ),
                            tint = if (running) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.cd_settings),
                            modifier = Modifier.size(30.dp),
                        )
                    }
                },
            )
        },
    ) { padding ->
        when (val s = state) {
            AppListState.Loading -> Centered(padding) { CircularProgressIndicator() }
            is AppListState.Ready ->
                if (s.apps.isEmpty()) {
                    Centered(padding) { Text(stringResource(R.string.empty_no_apps)) }
                } else {
                    AppList(
                        padding = padding,
                        apps = s.apps,
                        onToggle = vm::toggle,
                        onSetBlocked = vm::setBlocked,
                    )
                }
        }
    }
}

@Composable
private fun AppList(
    padding: PaddingValues,
    apps: List<AppUi>,
    onToggle: (String) -> Unit,
    onSetBlocked: (List<String>, Boolean) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val visible = remember(apps, query) {
        if (query.isBlank()) apps
        else apps.filter {
            it.label.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
        }
    }

    val allBlocked = apps.isNotEmpty() && apps.all { it.isBlocked }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            placeholder = { Text(stringResource(R.string.search_apps)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_clear))
                    }
                }
            },
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.blocked_count, apps.count { it.isBlocked }, apps.size),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick = { onSetBlocked(apps.map { it.packageName }, !allBlocked) },
            ) {
                Text(stringResource(if (allBlocked) R.string.unblock_all else R.string.block_all))
            }
        }

        if (visible.isEmpty()) {
            Centered(PaddingValues(0.dp)) { Text(stringResource(R.string.empty_no_matches)) }
        } else {
            val (blocked, allowed) = visible.partition { it.isBlocked }
            var allowedCollapsed by remember { mutableStateOf(false) }
            var blockedCollapsed by remember { mutableStateOf(false) }
            // Allowing an app moves it into the top group, so scroll up to reveal it.
            val listState = rememberLazyListState()
            var prevAllowed by remember { mutableStateOf(allowed.size) }
            LaunchedEffect(allowed.size) {
                if (allowed.size > prevAllowed) listState.animateScrollToItem(0)
                prevAllowed = allowed.size
            }
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                if (allowed.isNotEmpty()) {
                    item(key = "hdr-allowed") {
                        SectionHeader(stringResource(R.string.section_allowed), allowed.size, allowedCollapsed) {
                            allowedCollapsed = !allowedCollapsed
                        }
                    }
                    if (!allowedCollapsed) items(allowed, key = { it.packageName }) { app ->
                        AppRow(app, Modifier.animateItem()) { onToggle(app.packageName) }
                    }
                }
                if (blocked.isNotEmpty()) {
                    item(key = "hdr-blocked") {
                        SectionHeader(stringResource(R.string.section_blocked), blocked.size, blockedCollapsed) {
                            blockedCollapsed = !blockedCollapsed
                        }
                    }
                    if (!blockedCollapsed) items(blocked, key = { it.packageName }) { app ->
                        AppRow(app, Modifier.animateItem()) { onToggle(app.packageName) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(label: String, count: Int, collapsed: Boolean, onToggle: () -> Unit) {
    val rotation by animateFloatAsState(if (collapsed) 180f else 0f, label = "chevron")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onToggle() }
            .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(14.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Icon(
            imageVector = Icons.Filled.KeyboardArrowUp,
            contentDescription = stringResource(if (collapsed) R.string.cd_expand else R.string.cd_collapse),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = 8.dp)
                .rotate(rotation),
        )
    }
}

@Composable
private fun AppRow(app: AppUi, modifier: Modifier = Modifier, onToggle: () -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val icon = rememberAppIcon(app.packageName)
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(13.dp))
        ) {
            if (icon != null) {
                Image(
                    painter = icon,
                    contentDescription = null,
                    modifier = Modifier.matchParentSize()
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = app.packageName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = app.isBlocked, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun Centered(
    padding: PaddingValues,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun rememberVpnToggle(vm: MainViewModel, running: Boolean): () -> Unit {
    val consent = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) vm.startVpn()
    }
    return {
        if (running) {
            vm.stopVpn()
        } else {
            val intent = vm.vpnConsentIntent()
            if (intent != null) consent.launch(intent) else vm.startVpn()
        }
    }
}

// Decoded off-main and cached process-wide so scrolling back is a map hit, not
// a fresh PackageManager IPC + rasterize. Cache sized by bytes, decode bounded
// to render size so a big app list can't blow up memory.
private const val ICON_PX = 96 // ~44dp at xxhdpi
private val iconCache = object : LruCache<String, ImageBitmap>(8 * 1024 * 1024) {
    override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
}

@Composable
private fun rememberAppIcon(packageName: String): Painter? {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(iconCache[packageName], key1 = packageName) {
        if (value != null) return@produceState
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.packageManager.getApplicationIcon(packageName)
                    .toBitmap(ICON_PX, ICON_PX).asImageBitmap()
                    .also { iconCache.put(packageName, it) }
            }.getOrNull() // app uninstalled since load — leave blank
        }
    }
    return bitmap?.let { remember(it) { BitmapPainter(it) } }
}

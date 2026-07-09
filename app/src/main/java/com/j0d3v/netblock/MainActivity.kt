package com.j0d3v.netblock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.j0d3v.netblock.data.SettingsRepository
import com.j0d3v.netblock.data.ThemeSettings
import com.j0d3v.netblock.ui.NetblockApp
import com.j0d3v.netblock.ui.NetblockTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val repo = remember { SettingsRepository(applicationContext) }
            val settings by repo.themeSettings.collectAsStateWithLifecycle(
                initialValue = ThemeSettings(),
            )
            NetblockTheme(settings) {
                Surface { NetblockApp(repo) }
            }
        }
    }
}

package com.j0d3v.netblock.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SettingsRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun newRepo() = SettingsRepository(
        PreferenceDataStoreFactory.create { File(tmp.newFolder(), "test.preferences_pb") },
    )

    @Test
    fun onboarded_defaultsFalse_untilSet() = runTest {
        val repo = newRepo()
        assertFalse(repo.onboarded.first())
        repo.setOnboarded()
        assertTrue(repo.onboarded.first())
    }

    @Test
    fun themeSettings_defaults_thenPersistEachField() = runTest {
        val repo = newRepo()
        assertEquals(ThemeSettings(), repo.themeSettings.first())
        repo.setThemeMode(ThemeMode.DARK)
        repo.setAmoled(true)
        repo.setDynamicColor(false)
        assertEquals(
            ThemeSettings(mode = ThemeMode.DARK, amoled = true, dynamicColor = false),
            repo.themeSettings.first(),
        )
    }
}

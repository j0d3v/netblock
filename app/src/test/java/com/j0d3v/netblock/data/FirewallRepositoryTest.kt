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

class FirewallRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun newRepo() = FirewallRepository(
        PreferenceDataStoreFactory.create { File(tmp.newFolder(), "test.preferences_pb") },
    )

    @Test
    fun blockedPackages_defaultsEmpty() = runTest {
        assertTrue(newRepo().blockedPackages.first().isEmpty())
    }

    @Test
    fun toggle_addsThenRemoves() = runTest {
        val repo = newRepo()
        repo.toggle("com.foo")
        assertEquals(setOf("com.foo"), repo.blockedPackages.first())
        repo.toggle("com.foo")
        assertTrue(repo.blockedPackages.first().isEmpty())
    }

    @Test
    fun setBlocked_addsAndRemovesInBulk() = runTest {
        val repo = newRepo()
        repo.setBlocked(listOf("a", "b", "c"), blocked = true)
        assertEquals(setOf("a", "b", "c"), repo.blockedPackages.first())
        repo.setBlocked(listOf("b"), blocked = false)
        assertEquals(setOf("a", "c"), repo.blockedPackages.first())
    }

    @Test
    fun vpnEnabled_defaultsFalse_andPersists() = runTest {
        val repo = newRepo()
        assertFalse(repo.vpnEnabled.first())
        repo.setVpnEnabled(true)
        assertTrue(repo.vpnEnabled.first())
    }
}

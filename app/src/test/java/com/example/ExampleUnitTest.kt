package com.example

import com.example.data.catalog.SportModesCatalog
import com.example.watch.WatchH9BleManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun sportModesCatalog_hasOver100UniqueValidModes() {
        val modes = SportModesCatalog.allModes
        assertTrue("Expected at least 100 sport modes, got ${modes.size}", modes.size >= 100)
        assertEquals("Sport mode IDs must be unique", modes.size, modes.map { it.id }.distinct().size)
        assertEquals("Watch codes must be unique", modes.size, modes.map { it.watchCode }.distinct().size)
        modes.forEach { mode ->
            assertTrue(mode.id.length in 1..128)
            assertTrue(mode.name.length in 1..100)
            assertTrue(mode.category.length in 1..60)
            assertTrue(mode.targetHrMin in 40 until mode.targetHrMax)
            assertTrue(mode.metEstimate > 1.0)
        }
    }

    @Test
    fun compatibleDevices_includeWatchH9AndAllLaxasfitFamilies() {
        val families = WatchH9BleManager.compatibleFamilies
        assertTrue(families.any { it.displayName.contains("Watch H9") })
        assertTrue(families.any { it.displayName.contains("Laxasfit Watch") })
        assertTrue(families.any { it.displayName.contains("Laxasfit Band") })
        assertTrue(families.any { it.displayName.contains("Universal BLE") })
    }
}

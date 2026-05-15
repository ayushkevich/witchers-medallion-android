package by.alexy.witchersmedallion.permissions

import org.junit.Assert.assertTrue
import org.junit.Test

class GetBlePermissionsTest {

    @Test
    fun `getBlePermissions returns non-empty list`() {
        val permissions = getBlePermissions()
        assertTrue(permissions.isNotEmpty())
    }

    @Test
    fun `getBlePermissions contains required permissions`() {
        val permissions = getBlePermissions()
        assertTrue(permissions.any { it.contains("BLUETOOTH") || it.contains("LOCATION") })
    }
}

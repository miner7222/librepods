package me.kavishdevar.librepods.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Test

class BudBatteryByteTest {
    @Test
    fun `levels and the charging bit are read as sent`() {
        assertEquals(Pair(false, 58), BLEManager.formatBattery(58))
        assertEquals(Pair(true, 100), BLEManager.formatBattery(0x80 or 100))
    }

    @Test
    fun `a bud with no reading is not 127 percent`() {
        assertEquals(Pair(false, null), BLEManager.formatBattery(0x7F))
        assertEquals(Pair(false, null), BLEManager.formatBattery(0xFF))
    }
}

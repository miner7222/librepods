package me.kavishdevar.librepods.bluetooth

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AACPManagerEqTest {
    @Test fun invalidEqBandCountsAreRejectedBeforeSendingOrChangingState() {
        val manager = AACPManager()
        for (length in (0..16).filter { it != 8 }) {
            val error = assertThrows(IllegalArgumentException::class.java) {
                manager.sendPhoneMediaEQ(FloatArray(length))
            }
            assertEquals("EQ must be 8 floats", error.message)
            assertArrayEquals(FloatArray(8), manager.eqData, 0f)
        }
    }
}

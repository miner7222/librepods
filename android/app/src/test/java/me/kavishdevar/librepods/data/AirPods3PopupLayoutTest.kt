package me.kavishdevar.librepods.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AirPods3PopupLayoutTest {
    @Test
    fun usesMeasuredPopupLayout() {
        assertEquals(OverlayRingLayout(0.1900f, 0.3143f, 0.2804f, 0.6948f), AirPods3().ringLayout)
    }
}

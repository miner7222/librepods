package me.kavishdevar.librepods.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AirPods3PopupLayoutTest {
    @Test
    fun usesMeasuredPopupLayout() {
        assertEquals(OverlayRingLayout(0.2048f, 0.4090f, 0.3075f, 0.7135f), AirPods3().ringLayout)
    }
}

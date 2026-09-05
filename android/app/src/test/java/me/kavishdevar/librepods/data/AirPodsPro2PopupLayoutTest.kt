package me.kavishdevar.librepods.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AirPodsPro2PopupLayoutTest {
    @Test
    fun usesMeasuredPopupLayout() {
        assertEquals(OverlayRingLayout(0.1829f, 0.3357f, 0.2231f, 0.7018f), AirPodsPro2Lightning().ringLayout)
        assertEquals(AirPodsPro2Lightning().ringLayout, AirPodsPro2USBC().ringLayout)
    }
}

package me.kavishdevar.librepods.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AirPodsPro2PopupLayoutTest {
    @Test
    fun usesMeasuredPopupLayout() {
        assertEquals(OverlayRingLayout(0.1714f, 0.3790f, 0.2752f, 0.7143f), AirPodsPro2Lightning().ringLayout)
        assertEquals(AirPodsPro2Lightning().ringLayout, AirPodsPro2USBC().ringLayout)
    }
}

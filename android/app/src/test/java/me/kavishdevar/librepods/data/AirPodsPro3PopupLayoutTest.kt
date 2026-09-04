package me.kavishdevar.librepods.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AirPodsPro3PopupLayoutTest {
    @Test
    fun ringsFollowTheMeasuredPro3Popup() {
        val expected = OverlayRingLayout(0.1988f, 0.3501f, 0.2997f, 0.6929f)
        for (number in listOf("A3063", "A3064", "A3065")) {
            assertEquals(expected, AirPodsModels.getModelByModelNumber(number)?.ringLayout)
        }
    }
}

package me.kavishdevar.librepods.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AirPodsPro3PopupLayoutTest {
    @Test
    fun ringsFollowTheSharedWideStillComposition() {
        val expected = OverlayRingLayout(0.1748f, 0.3614f, 0.2681f, 0.6952f)
        for (number in listOf("A3063", "A3064", "A3065")) {
            assertEquals(expected, AirPodsModels.getModelByModelNumber(number)?.ringLayout)
        }
    }
}

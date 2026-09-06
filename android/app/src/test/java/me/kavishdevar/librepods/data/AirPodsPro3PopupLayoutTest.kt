package me.kavishdevar.librepods.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AirPodsPro3PopupLayoutTest {
    @Test
    fun ringsFollowTheSharedWideStillComposition() {
        val expected = OverlayRingLayout(0.2034f, 0.3778f, 0.2906f, 0.6803f)
        for (number in listOf("A3063", "A3064", "A3065")) {
            assertEquals(expected, AirPodsModels.getModelByModelNumber(number)?.ringLayout)
        }
    }
}

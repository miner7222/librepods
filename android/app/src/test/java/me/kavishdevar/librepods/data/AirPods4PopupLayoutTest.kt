package me.kavishdevar.librepods.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AirPods4PopupLayoutTest {
    @Test
    fun ringsFollowTheOriginalIosPopup() {
        val expected = OverlayRingLayout(0.1860f, 0.3416f, 0.2631f, 0.6873f)
        for (number in listOf("A3053", "A3050", "A3054", "A3056", "A3055", "A3057")) {
            assertEquals(expected, AirPodsModels.getModelByModelNumber(number)?.ringLayout)
        }
    }
}

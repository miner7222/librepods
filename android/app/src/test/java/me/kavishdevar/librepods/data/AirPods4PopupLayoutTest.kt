package me.kavishdevar.librepods.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AirPods4PopupLayoutTest {
    @Test
    fun ringsFollowSharedStillAndMotionCompositions() {
        val expected = OverlayRingLayout(0.2052f, 0.4090f, 0.3075f, 0.7135f, 0.2582f, 0.6905f)
        for (number in listOf("A3053", "A3050", "A3054", "A3056", "A3055", "A3057")) {
            assertEquals(expected, AirPodsModels.getModelByModelNumber(number)?.ringLayout)
        }
    }
}

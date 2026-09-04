package me.kavishdevar.librepods.data

import me.kavishdevar.librepods.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AirPodsArtworkTest {
    @Test
    fun airPods4AndAncUseTheirOwnArtwork() {
        for (number in listOf("A3053", "A3050", "A3054", "A3056", "A3055", "A3057")) {
            val model = requireNotNull(AirPodsModels.getModelByModelNumber(number))
            assertEquals(R.drawable.airpods_4, model.budCaseRes)
            assertEquals(R.drawable.airpods_4_buds, model.budsRes)
            assertEquals(R.drawable.airpods_4_left, model.leftBudsRes)
            assertEquals(R.drawable.airpods_4_right, model.rightBudsRes)
            assertEquals(R.drawable.airpods_4_case, model.caseRes)
            assertEquals(R.raw.airpods_4_connected, model.connectedVideoRes)
            assertEquals(R.raw.airpods_4_island, model.islandVideoRes)
        }
    }

    @Test
    fun airPodsPro2UsesItsOwnArtwork() {
        for (number in listOf("A2931", "A2699", "A2698")) {
            val model = requireNotNull(AirPodsModels.getModelByModelNumber(number))
            assertEquals(R.drawable.airpods_pro_2, model.budCaseRes)
            assertEquals(R.raw.airpods_pro_2_connected, model.connectedVideoRes)
            assertEquals(R.raw.airpods_pro_2_island, model.islandVideoRes)
        }
    }

    @Test
    fun everyStandardAirPodsWithoutArtworkBorrowsTheAirPods4() {
        // AirPods 1, 2 and 3
        for (number in listOf("A1523", "A1722", "A2032", "A2031", "A2565", "A2564")) {
            val model = requireNotNull(AirPodsModels.getModelByModelNumber(number))
            assertEquals(FallbackArtwork.Standard.budCase, model.budCaseRes)
            assertEquals(FallbackArtwork.Standard.buds, model.budsRes)
            assertEquals(FallbackArtwork.Standard.leftBuds, model.leftBudsRes)
            assertEquals(FallbackArtwork.Standard.rightBuds, model.rightBudsRes)
            assertEquals(FallbackArtwork.Standard.chargingCase, model.caseRes)
            assertEquals(FallbackArtwork.Standard.connected, model.connectedVideoRes)
            assertEquals(FallbackArtwork.Standard.island, model.islandVideoRes)
        }
    }

    @Test
    fun everyProWithoutArtworkBorrowsTheAirPodsPro2() {
        // AirPods Pro 1
        for (number in listOf("A2084", "A2083")) {
            val model = requireNotNull(AirPodsModels.getModelByModelNumber(number))
            assertEquals(FallbackArtwork.Pro.budCase, model.budCaseRes)
            assertEquals(FallbackArtwork.Pro.buds, model.budsRes)
            assertEquals(FallbackArtwork.Pro.leftBuds, model.leftBudsRes)
            assertEquals(FallbackArtwork.Pro.rightBuds, model.rightBudsRes)
            assertEquals(FallbackArtwork.Pro.chargingCase, model.caseRes)
            assertEquals(FallbackArtwork.Pro.connected, model.connectedVideoRes)
            assertEquals(FallbackArtwork.Pro.island, model.islandVideoRes)
        }
    }

    @Test
    fun theTwoFallbackSetsKeepTheirExistingArtwork() {
        assertEquals(R.raw.airpods_4_connected, FallbackArtwork.Standard.connected)
        assertEquals(R.raw.airpods_4_island, FallbackArtwork.Standard.island)
        assertEquals(R.raw.airpods_pro_2_connected, FallbackArtwork.Pro.connected)
        assertEquals(R.raw.airpods_pro_2_island, FallbackArtwork.Pro.island)
    }

    @Test
    fun airPodsPro3UsesItsOwnArtworkForEveryModelNumberAndBroadcastName() {
        val models = listOf("A3063", "A3064", "A3065").map {
            requireNotNull(AirPodsModels.getModelByModelNumber(it))
        } + requireNotNull(AirPodsModels.getModelForOverlays("", "AirPods Pro 3"))
        for (model in models) {
            assertEquals(R.drawable.airpods_pro_3, model.budCaseRes)
            assertEquals(R.drawable.airpods_pro_3_buds, model.budsRes)
            assertEquals(R.drawable.airpods_pro_3_left, model.leftBudsRes)
            assertEquals(R.drawable.airpods_pro_3_right, model.rightBudsRes)
            assertEquals(R.drawable.airpods_pro_3_case, model.caseRes)
            assertEquals(R.raw.airpods_pro_3_connected, model.connectedVideoRes)
            assertEquals(R.raw.airpods_pro_3_island, model.islandVideoRes)
        }
    }

    @Test
    fun overlaysCanResolveAirPods4BeforeModelNumberArrives() {
        for (name in listOf("AirPods 4", "AirPods 4 (ANC)")) {
            val model = requireNotNull(AirPodsModels.getModelForOverlays("", name))
            assertEquals(R.raw.airpods_4_connected, model.connectedVideoRes)
        }
    }

    @Test
    fun overlaysCanResolveAirPods3BeforeModelNumberArrives() {
        val model = requireNotNull(AirPodsModels.getModelForOverlays("", "AirPods 3"))
        assertEquals(FallbackArtwork.Standard.connected, model.connectedVideoRes)
    }

    @Test
    fun overlaysCanResolveAirPodsPro1FromItsBroadcastName() {
        val model = requireNotNull(AirPodsModels.getModelForOverlays("", "AirPods Pro"))
        assertEquals("AirPods Pro 1", model.name)
        assertEquals(FallbackArtwork.Pro.connected, model.connectedVideoRes)
    }

    @Test
    fun modelNumberTakesPrecedenceOverBroadcastName() {
        val model = requireNotNull(AirPodsModels.getModelForOverlays("A3056", "AirPods 4"))
        assertEquals("AirPods 4 (ANC)", model.name)
    }

    @Test
    fun unknownModelsHaveNoOverride() {
        assertNull(AirPodsModels.getModelForOverlays("unknown", null))
        assertNull(AirPodsModels.getModelForOverlays("", "My AirPods 4"))
    }
}

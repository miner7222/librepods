/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package me.kavishdevar.librepods.data

import me.kavishdevar.librepods.R

open class AirPodsBase(
    val modelNumber: List<String>,
    val name: String,
    val displayName: String = "AirPods",
    val manufacturer: String = "Apple Inc.",
    val budCaseRes: Int,
    val budsRes: Int,
    val leftBudsRes: Int,
    val rightBudsRes: Int,
    val caseRes: Int,
    val capabilities: Set<Capability>,
    val connectedVideoRes: Int,
    val islandVideoRes: Int
)

/**
 * Artwork borrowed by a model that has none of its own.
 *
 * Only two sets exist: the AirPods 4 for the standard shape and the AirPods Pro 2 for the
 * Pro shape. Every other generation points at whichever matches its shape. Going through
 * here rather than naming the resource directly is what marks the artwork as borrowed - a
 * model that owns its assets names them itself.
 */
object FallbackArtwork {
    /** Any standard AirPods without renders of its own. */
    object Standard {
        val budCase = R.drawable.airpods_4
        val buds = R.drawable.airpods_4_buds
        val leftBuds = R.drawable.airpods_4_left
        val rightBuds = R.drawable.airpods_4_right
        val chargingCase = R.drawable.airpods_4_case
        val connected = R.raw.airpods_4_connected
        val island = R.raw.airpods_4_island
    }

    /**
     * Any AirPods Pro without artwork of its own. These clips came from upstream with no
     * known provenance; the spacing and tone are ours, recomposited to the reference popup.
     */
    object Pro {
        val budCase = R.drawable.airpods_pro_2
        val buds = R.drawable.airpods_pro_2_buds
        val leftBuds = R.drawable.airpods_pro_2_left
        val rightBuds = R.drawable.airpods_pro_2_right
        val chargingCase = R.drawable.airpods_pro_2_case
        val connected = R.raw.airpods_pro_2_connected
        val island = R.raw.airpods_pro_2_island
    }
}
enum class Capability {
    LISTENING_MODE,
    CONVERSATION_AWARENESS,
    STEM_CONFIG,
    HEAD_GESTURES,
    LOUD_SOUND_REDUCTION,
    PPE,
    SLEEP_DETECTION,
    HEARING_AID,
    ADAPTIVE_AUDIO,
    ADAPTIVE_VOLUME,
    SWIPE_FOR_VOLUME,
    HRM
}

class AirPods: AirPodsBase(
    modelNumber = listOf("A1523", "A1722"),
    name = "AirPods 1",
    // budCaseRes = R.drawable.airpods_1
    budCaseRes = FallbackArtwork.Standard.budCase,
    budsRes = FallbackArtwork.Standard.buds,
    leftBudsRes = FallbackArtwork.Standard.leftBuds,
    rightBudsRes = FallbackArtwork.Standard.rightBuds,
    caseRes = FallbackArtwork.Standard.chargingCase,
    connectedVideoRes = FallbackArtwork.Standard.connected,
    islandVideoRes = FallbackArtwork.Standard.island,
    capabilities = emptySet()
)

class AirPods2: AirPodsBase(
    modelNumber = listOf("A2032", "A2031"),
    name = "AirPods 2",
    // budCaseRes = R.drawable.airpods_2
    budCaseRes = FallbackArtwork.Standard.budCase,
    budsRes = FallbackArtwork.Standard.buds,
    leftBudsRes = FallbackArtwork.Standard.leftBuds,
    rightBudsRes = FallbackArtwork.Standard.rightBuds,
    caseRes = FallbackArtwork.Standard.chargingCase,
    connectedVideoRes = FallbackArtwork.Standard.connected,
    islandVideoRes = FallbackArtwork.Standard.island,
    capabilities = emptySet()
)

class AirPods3: AirPodsBase(
    modelNumber = listOf("A2565", "A2564"),
    name = "AirPods 3",
    budCaseRes = FallbackArtwork.Standard.budCase,
    budsRes = FallbackArtwork.Standard.buds,
    leftBudsRes = FallbackArtwork.Standard.leftBuds,
    rightBudsRes = FallbackArtwork.Standard.rightBuds,
    caseRes = FallbackArtwork.Standard.chargingCase,
    connectedVideoRes = FallbackArtwork.Standard.connected,
    islandVideoRes = FallbackArtwork.Standard.island,
    capabilities = setOf(
        Capability.HEAD_GESTURES
    )
)

class AirPods4: AirPodsBase(
    modelNumber = listOf("A3053", "A3050", "A3054"),
    name = "AirPods 4",
    budCaseRes = R.drawable.airpods_4,
    budsRes = R.drawable.airpods_4_buds,
    leftBudsRes = R.drawable.airpods_4_left,
    rightBudsRes = R.drawable.airpods_4_right,
    caseRes = R.drawable.airpods_4_case,
    connectedVideoRes = R.raw.airpods_4_connected,
    islandVideoRes = R.raw.airpods_4_island,
    capabilities = setOf(
        Capability.HEAD_GESTURES,
        Capability.SLEEP_DETECTION,
        Capability.ADAPTIVE_VOLUME
    )
)

class AirPods4ANC: AirPodsBase(
    modelNumber = listOf("A3056", "A3055", "A3057"),
    name = "AirPods 4 (ANC)",
    budCaseRes = R.drawable.airpods_4,
    budsRes = R.drawable.airpods_4_buds,
    leftBudsRes = R.drawable.airpods_4_left,
    rightBudsRes = R.drawable.airpods_4_right,
    caseRes = R.drawable.airpods_4_case,
    connectedVideoRes = R.raw.airpods_4_connected,
    islandVideoRes = R.raw.airpods_4_island,
    capabilities = setOf(
        Capability.LISTENING_MODE,
        Capability.CONVERSATION_AWARENESS,
        Capability.HEAD_GESTURES,
        Capability.ADAPTIVE_AUDIO,
        Capability.SLEEP_DETECTION,
        Capability.ADAPTIVE_VOLUME,
        Capability.STEM_CONFIG
    )
)

class AirPodsPro1: AirPodsBase(
    modelNumber = listOf("A2084", "A2083"),
    name = "AirPods Pro 1",
    displayName = "AirPods Pro",
    budCaseRes = FallbackArtwork.Pro.budCase,
    budsRes = FallbackArtwork.Pro.buds,
    leftBudsRes = FallbackArtwork.Pro.leftBuds,
    rightBudsRes = FallbackArtwork.Pro.rightBuds,
    caseRes = FallbackArtwork.Pro.chargingCase,
    connectedVideoRes = FallbackArtwork.Pro.connected,
    islandVideoRes = FallbackArtwork.Pro.island,
    capabilities = setOf(
        Capability.LISTENING_MODE
    )
)

class AirPodsPro2Lightning: AirPodsBase(
    modelNumber = listOf("A2931", "A2699", "A2698"),
    name = "AirPods Pro 2 with Magsafe Charging Case (Lightning)",
    displayName = "AirPods Pro",
    // budCaseRes = R.drawable.airpods_pro_2
    budCaseRes = R.drawable.airpods_pro_2,
    // budsRes = R.drawable.airpods_pro_2_buds
    budsRes = R.drawable.airpods_pro_2_buds,
    // leftBudsRes = R.drawable.airpods_pro_2_left
    leftBudsRes = R.drawable.airpods_pro_2_left,
    // rightBudsRes = R.drawable.airpods_pro_2_right
    rightBudsRes = R.drawable.airpods_pro_2_right,
    // caseRes = R.drawable.airpods_pro_2_case
    caseRes = R.drawable.airpods_pro_2_case,
    connectedVideoRes = R.raw.airpods_pro_2_connected,
    islandVideoRes = R.raw.airpods_pro_2_island,
    capabilities = setOf(
        Capability.LISTENING_MODE,
        Capability.CONVERSATION_AWARENESS,
        Capability.STEM_CONFIG,
        Capability.LOUD_SOUND_REDUCTION,
        Capability.SLEEP_DETECTION,
        Capability.HEARING_AID,
        Capability.ADAPTIVE_AUDIO,
        Capability.ADAPTIVE_VOLUME,
        Capability.SWIPE_FOR_VOLUME,
        Capability.HEAD_GESTURES
    )
)

class AirPodsPro2USBC: AirPodsBase(
    modelNumber = listOf("A3047", "A3048", "A3049"),
    name = "AirPods Pro 2 with Magsafe Charging Case (USB-C)",
    displayName = "AirPods Pro",
    // budCaseRes = R.drawable.airpods_pro_2
    budCaseRes = R.drawable.airpods_pro_2,
    // budsRes = R.drawable.airpods_pro_2_buds
    budsRes = R.drawable.airpods_pro_2_buds,
    // leftBudsRes = R.drawable.airpods_pro_2_left
    leftBudsRes = R.drawable.airpods_pro_2_left,
    // rightBudsRes = R.drawable.airpods_pro_2_right
    rightBudsRes = R.drawable.airpods_pro_2_right,
    // caseRes = R.drawable.airpods_pro_2_case
    caseRes = R.drawable.airpods_pro_2_case,
    connectedVideoRes = R.raw.airpods_pro_2_connected,
    islandVideoRes = R.raw.airpods_pro_2_island,
    capabilities = setOf(
        Capability.LISTENING_MODE,
        Capability.CONVERSATION_AWARENESS,
        Capability.STEM_CONFIG,
        Capability.LOUD_SOUND_REDUCTION,
        Capability.SLEEP_DETECTION,
        Capability.HEARING_AID,
        Capability.ADAPTIVE_AUDIO,
        Capability.ADAPTIVE_VOLUME,
        Capability.SWIPE_FOR_VOLUME,
        Capability.HEAD_GESTURES
    )
)

class AirPodsPro3: AirPodsBase(
    modelNumber = listOf("A3063", "A3064", "A3065"),
    name = "AirPods Pro 3",
    displayName = "AirPods Pro",
    // budCaseRes = R.drawable.airpods_pro_3
    budCaseRes = FallbackArtwork.Pro.budCase,
    budsRes = FallbackArtwork.Pro.buds,
    leftBudsRes = FallbackArtwork.Pro.leftBuds,
    rightBudsRes = FallbackArtwork.Pro.rightBuds,
    caseRes = FallbackArtwork.Pro.chargingCase,
    connectedVideoRes = FallbackArtwork.Pro.connected,
    islandVideoRes = FallbackArtwork.Pro.island,
    capabilities = setOf(
        Capability.LISTENING_MODE,
        Capability.CONVERSATION_AWARENESS,
        Capability.HEAD_GESTURES,
        Capability.STEM_CONFIG,
        Capability.LOUD_SOUND_REDUCTION,
        Capability.PPE,
        Capability.SLEEP_DETECTION,
        Capability.HEARING_AID,
        Capability.ADAPTIVE_AUDIO,
        Capability.ADAPTIVE_VOLUME,
        Capability.SWIPE_FOR_VOLUME,
        Capability.HRM
    )
)

data class AirPodsInstance(
    val name: String,
    val model: AirPodsBase,
    val actualModelNumber: String,
    val serialNumber: String?,
    val leftSerialNumber: String?,
    val rightSerialNumber: String?,
    val version1: String?,
    val version2: String?,
    val version3: String?,
)

object AirPodsModels {
    val models: List<AirPodsBase> = listOf(
        AirPods(),
        AirPods2(),
        AirPods3(),
        AirPods4(),
        AirPods4ANC(),
        AirPodsPro1(),
        AirPodsPro2Lightning(),
        AirPodsPro2USBC(),
        AirPodsPro3()
    )

    fun getModelByModelNumber(modelNumber: String): AirPodsBase? {
        return models.find { modelNumber in it.modelNumber }
    }

    fun getModelForOverlays(modelNumber: String, broadcastModelName: String?): AirPodsBase? {
        return getModelByModelNumber(modelNumber)
            ?: models.find { it.name == broadcastModelName }
            ?: models.find { it is AirPodsPro1 && broadcastModelName == "AirPods Pro" }
    }
}

package me.kavishdevar.librepods.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EqPayloadsTest {
    private fun hearingAidSettings() = HearingAidSettings(
        leftEQ = FloatArray(8) { it / 8f }, rightEQ = FloatArray(8) { -it / 8f },
        leftAmplification = 0.25f, rightAmplification = 0.5f,
        leftTone = 0.1f, rightTone = 0.2f,
        leftConversationBoost = true, rightConversationBoost = false,
        leftAmbientNoiseReduction = 0.3f, rightAmbientNoiseReduction = 0.4f,
        netAmplification = 0.375f, balance = 0.25f, ownVoiceAmplification = 0.75f
    )

    private fun transparencySettings(ownVoice: Float? = null) = TransparencySettings(
        enabled = true,
        leftEQ = FloatArray(8) { it / 8f }, rightEQ = FloatArray(8) { -it / 8f },
        leftAmplification = 0.25f, rightAmplification = 0.5f,
        leftTone = 0.1f, rightTone = 0.2f,
        leftConversationBoost = true, rightConversationBoost = false,
        leftAmbientNoiseReduction = 0.3f, rightAmbientNoiseReduction = 0.4f,
        netAmplification = 0.375f, balance = 0.25f, ownVoiceAmplification = ownVoice
    )

    @Test fun hearingAidWriteCopiesTheCacheAndPreservesHeaderAndExtensions() {
        val cache = ByteArray(108) { (it * 3).toByte() }
        val original = cache.copyOf()
        val settings = hearingAidSettings()
        val payload = buildHearingAidSettingsPayload(cache, settings)!!
        assertNotSame(cache, payload)
        assertArrayEquals(original, cache)
        assertEquals(original.size, payload.size)
        for (index in listOf(0, 1, 3, 104, 105, 106, 107)) {
            assertEquals(original[index], payload[index])
        }
        assertEquals(0x64.toByte(), payload[2])
        assertEquals(settings, parseHearingAidSettingsResponse(payload))
        payload.fill(0)
        assertArrayEquals(original, cache)
    }

    @Test fun hearingAidPayloadIsASnapshotBeforeTheDebouncedSend() {
        val cache = ByteArray(104) { 0x55 }
        val settings = hearingAidSettings()
        val payload = buildHearingAidSettingsPayload(cache, settings)!!
        val snapshot = payload.copyOf()
        cache.fill(0)
        settings.leftEQ.fill(99f)
        settings.rightEQ.fill(-99f)
        assertArrayEquals(snapshot, payload)
    }

    @Test fun hearingAidRejectsEveryTruncatedPayload() {
        for (length in 0 until 104) {
            val data = ByteArray(length)
            assertNull(buildHearingAidSettingsPayload(data, hearingAidSettings()))
            assertNull(parseHearingAidSettingsResponse(data))
        }
    }

    @Test fun hearingAidRejectsShortAndOversizedEqInEitherEarWithoutMutatingTheCache() {
        val cache = ByteArray(104) { 0x55 }
        val original = cache.copyOf()
        val settings = hearingAidSettings()
        for (length in (0..16).filter { it != 8 }) {
            assertNull(buildHearingAidSettingsPayload(cache, settings.copy(leftEQ = FloatArray(length))))
            assertNull(buildHearingAidSettingsPayload(cache, settings.copy(rightEQ = FloatArray(length))))
            assertArrayEquals(original, cache)
        }
    }

    @Test fun transparencyRejectsEveryTruncatedBasePayload() {
        for (length in 0 until 100) {
            assertNull(parseTransparencySettingsResponse(ByteArray(length)))
        }
    }

    @Test fun transparencyRejectsShortAndOversizedEqInEitherEar() {
        for (ownVoice in listOf(null, 0.75f)) {
            val settings = transparencySettings(ownVoice)
            for (length in (0..16).filter { it != 8 }) {
                assertNull(buildTransparencySettingsPayload(settings.copy(leftEQ = FloatArray(length))))
                assertNull(buildTransparencySettingsPayload(settings.copy(rightEQ = FloatArray(length))))
            }
        }
    }

    @Test fun transparencyRoundTripsBothSupportedWireLengths() {
        for (ownVoice in listOf(null, 0.75f)) {
            val settings = transparencySettings(ownVoice)
            val payload = buildTransparencySettingsPayload(settings)!!
            assertEquals(if (ownVoice == null) 100 else 104, payload.size)
            val parsed = parseTransparencySettingsResponse(payload)!!
            assertTrue(parsed.enabled)
            assertEquals(settings, parsed)
            assertArrayEquals(settings.leftEQ, parsed.leftEQ, 0f)
            assertArrayEquals(settings.rightEQ, parsed.rightEQ, 0f)
            assertEquals(ownVoice, parsed.ownVoiceAmplification)
        }
    }

    @Test fun restoredEqRequiresExactlyEightBandsAndDoesNotShareItsInput() {
        for (length in (0..16).filter { it != 8 }) {
            assertNull(restoreEqBands(List(length) { 0.5f }))
        }
        val saved = MutableList(8) { it / 8f }
        val restored = restoreEqBands(saved)!!
        val snapshot = restored.copyOf()
        saved.fill(99f)
        assertArrayEquals(snapshot, restored, 0f)
    }
}

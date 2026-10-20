package me.kavishdevar.librepods.utils

import android.media.AudioAttributes
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaPlaybackTest {
    @Test fun acceptsUntypedMediaPlayers() {
        assertTrue(isMediaPlayback(AudioAttributes.CONTENT_TYPE_UNKNOWN, AudioAttributes.USAGE_MEDIA))
    }

    @Test fun preservesTypedMusicAndMovieDetectionRegardlessOfUsage() {
        for (usage in listOf(
            AudioAttributes.USAGE_UNKNOWN, AudioAttributes.USAGE_MEDIA,
            AudioAttributes.USAGE_ALARM, AudioAttributes.USAGE_VOICE_COMMUNICATION
        )) {
            assertTrue(isMediaPlayback(AudioAttributes.CONTENT_TYPE_MUSIC, usage))
            assertTrue(isMediaPlayback(AudioAttributes.CONTENT_TYPE_MOVIE, usage))
        }
    }

    @Test fun untypedNonMediaAudioDoesNotTriggerTakeover() {
        for (usage in listOf(
            AudioAttributes.USAGE_UNKNOWN, AudioAttributes.USAGE_ALARM,
            AudioAttributes.USAGE_NOTIFICATION, AudioAttributes.USAGE_VOICE_COMMUNICATION,
            AudioAttributes.USAGE_GAME, AudioAttributes.USAGE_ASSISTANT
        )) {
            assertFalse(isMediaPlayback(AudioAttributes.CONTENT_TYPE_UNKNOWN, usage))
        }
    }

    @Test fun mediaUsageDoesNotTurnOtherContentTypesIntoMusic() {
        assertFalse(isMediaPlayback(AudioAttributes.CONTENT_TYPE_SPEECH, AudioAttributes.USAGE_MEDIA))
        assertFalse(isMediaPlayback(AudioAttributes.CONTENT_TYPE_SONIFICATION, AudioAttributes.USAGE_MEDIA))
    }

    @Test fun untypedMediaMustRemainActiveAcrossTheSustainWindow() {
        val gate = UntypedMediaPlaybackGate(300L)
        assertFalse(gate.accepts(false, true, true, 1_000L))
        assertFalse(gate.accepts(false, true, true, 1_299L))
        assertTrue(gate.accepts(false, true, true, 1_300L))
        assertTrue(gate.accepts(false, true, true, 1_600L))
    }

    @Test fun shortUiSoundCannotPassTheTrailingRecheck() {
        val gate = UntypedMediaPlaybackGate(300L)
        assertFalse(gate.accepts(false, true, true, 1_000L))
        assertFalse(gate.accepts(false, true, false, 1_320L))
        // A subsequent UI click starts a new window instead of inheriting the first one.
        assertFalse(gate.accepts(false, true, true, 1_500L))
        assertFalse(gate.accepts(false, true, false, 1_820L))
    }

    @Test fun inactiveCallbackDuringDebounceResetsTheWindow() {
        val gate = UntypedMediaPlaybackGate(300L)
        assertFalse(gate.accepts(false, true, true, 1_000L))
        gate.reset()
        assertFalse(gate.accepts(false, true, true, 1_320L))
        assertTrue(gate.accepts(false, true, true, 1_620L))
    }

    @Test fun disappearingMediaConfigurationResetsTheWindow() {
        val gate = UntypedMediaPlaybackGate(300L)
        assertFalse(gate.accepts(false, true, true, 1_000L))
        assertFalse(gate.accepts(false, false, true, 1_320L))
        assertFalse(gate.accepts(false, true, true, 1_500L))
    }

    @Test fun typedMusicAndMoviesDoNotWaitForTheUntypedSustainWindow() {
        val gate = UntypedMediaPlaybackGate(300L)
        assertTrue(gate.accepts(true, false, true, 1_000L))
        assertTrue(gate.accepts(true, true, true, 1_001L))
        assertFalse(gate.accepts(false, true, true, 1_002L))
    }
}

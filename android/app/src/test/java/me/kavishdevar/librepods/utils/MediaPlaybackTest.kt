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
}

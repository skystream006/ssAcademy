package com.ssmath.app

import android.app.Application
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.test.core.app.ApplicationProvider
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowTextToSpeech

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class OfflineReadingSpeakerTest {
    private fun voice(name: String, locale: Locale = Locale.US, network: Boolean = false,
        features: Set<String> = emptySet()) =
        Voice(name, locale, Voice.QUALITY_NORMAL, Voice.LATENCY_NORMAL, network, features)

    @Test fun onlyInstalledOfflineEnglishVoicesAreEligible() {
        val online = voice("online", network = true)
        val missing = voice("not-installed", features = setOf(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED))
        val otherLanguage = voice("french", locale = Locale.FRANCE)
        assertNull(offlineReadingVoice(setOf(online, missing, otherLanguage)))
        val offline = voice("english", locale = Locale.UK)
        assertEquals(offline, offlineReadingVoice(setOf(online, missing, otherLanguage, offline)))
    }

    @Test fun voiceSelectionIsStableAndPrefersContentLocale() {
        val british = voice("a-british", locale = Locale.UK)
        val american = voice("b-american")
        assertEquals(american, offlineReadingVoice(linkedSetOf(british, american)))
        assertEquals(american, offlineReadingVoice(linkedSetOf(american, british)))
        assertNull(offlineReadingVoice(emptySet()))
    }

    @Test fun missingSpeechServiceAndLateInitializationCannotBreakReading() {
        val speaker = OfflineReadingSpeaker(ApplicationProvider.getApplicationContext<Application>())
        val speech = shadowOf(ShadowTextToSpeech.getLastTextToSpeechInstance())
        assertFalse(speaker.speak("A short story."))
        speaker.close()
        speech.onInitListener.onInit(TextToSpeech.SUCCESS)
        shadowOf(Looper.getMainLooper()).idle()
        assertFalse(speaker.available)
        assertFalse(speaker.speak("A short story."))
        assertTrue(speech.isShutdown)
        speaker.stop()
        speaker.close()
    }

    @Test fun successfulNarrationUsesOfflineVoiceAndStopsOnClose() {
        val offline = voice("offline")
        ShadowTextToSpeech.addVoice(voice("cloud", network = true))
        ShadowTextToSpeech.addVoice(offline)
        val speaker = OfflineReadingSpeaker(ApplicationProvider.getApplicationContext<Application>())
        val speech = shadowOf(ShadowTextToSpeech.getLastTextToSpeechInstance())
        speech.onInitListener.onInit(TextToSpeech.SUCCESS)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(speaker.available)
        assertEquals(offline, speech.currentVoice)
        assertTrue(speaker.speak("The kite rises high."))
        assertEquals("The kite rises high.", speech.lastSpokenText)
        assertEquals(TextToSpeech.QUEUE_FLUSH, speech.queueMode)
        assertFalse(speaker.speak(" "))
        assertFalse(speaker.speak("x".repeat(TextToSpeech.getMaxSpeechInputLength() + 1)))
        speaker.stop()
        assertTrue(speech.isStopped)
        speaker.close()
        assertTrue(speech.isShutdown)
    }
}

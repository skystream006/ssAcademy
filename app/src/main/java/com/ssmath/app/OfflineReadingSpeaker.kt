package com.ssmath.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.Locale

internal fun offlineReadingVoice(voices: Set<Voice>): Voice? = voices
    .filter {
        it.locale.language == Locale.ENGLISH.language && !it.isNetworkConnectionRequired &&
            TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features.orEmpty()
    }
    .sortedWith(compareByDescending<Voice> { it.locale == Locale.US }.thenBy { it.name })
    .firstOrNull()

class OfflineReadingSpeaker(context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private var engine: TextToSpeech? = null
    private var closed = false
    private var utterance = 0L
    var available by mutableStateOf(false)
        private set

    init {
        engine = try {
            TextToSpeech(context.applicationContext) { status ->
                // Initialization may arrive after the Reading screen has been closed.
                handler.post {
                    if (!closed && status == TextToSpeech.SUCCESS) {
                        available = try {
                            val speech = engine
                            val voice = offlineReadingVoice(speech?.voices.orEmpty())
                            voice != null && speech?.setVoice(voice) == TextToSpeech.SUCCESS
                        } catch (_: RuntimeException) {
                            false
                        }
                    }
                }
            }
        } catch (_: RuntimeException) {
            null
        }
    }

    fun speak(text: String): Boolean {
        if (closed || !available || text.isBlank() || text.length > TextToSpeech.getMaxSpeechInputLength()) return false
        return try {
            engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "reading-${++utterance}") == TextToSpeech.SUCCESS
        } catch (_: RuntimeException) {
            available = false
            false
        }
    }

    fun stop() {
        try {
            engine?.stop()
        } catch (_: RuntimeException) {
            available = false
        }
    }

    internal fun close() {
        closed = true
        available = false
        handler.removeCallbacksAndMessages(null)
        stop()
        try {
            engine?.shutdown()
        } catch (_: RuntimeException) {
            // Reading remains usable when an installed speech service fails.
        }
        engine = null
    }
}

@Composable
fun rememberOfflineReadingSpeaker(): OfflineReadingSpeaker {
    val context = LocalContext.current.applicationContext
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val speaker = remember(context, lifecycle) { OfflineReadingSpeaker(context) }
    DisposableEffect(speaker, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) speaker.stop()
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            speaker.close()
        }
    }
    return speaker
}

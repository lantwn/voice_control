package com.voicerider.voice.engine

import android.content.Context
import android.speech.tts.TextToSpeech
import com.voicerider.core.util.Logger
import com.voicerider.voice.config.VoiceConfig
import java.util.Locale

class TtsSpeaker(private val context: Context) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    fun initialize(): Boolean {
        tts = TextToSpeech(context) { status ->
            isInitialized = (status == TextToSpeech.SUCCESS)
            if (isInitialized) {
                tts?.language = Locale.CHINESE
                tts?.setSpeechRate(VoiceConfig.TTS_SPEED / 50f)
                tts?.setPitch(VoiceConfig.TTS_PITCH / 50f)
            }
        }
        return true
    }

    fun speak(text: String) {
        if (!isInitialized) {
            Logger.w("TtsSpeaker: not initialized")
            return
        }
        Logger.i("TtsSpeaker: speaking '$text'")
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "voice_rider_tts")
    }

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
    }
}

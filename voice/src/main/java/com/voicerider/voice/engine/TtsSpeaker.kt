package com.voicerider.voice.engine

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.voicerider.core.util.Logger
import com.voicerider.voice.config.VoiceConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

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

    /** 异步播报（不等待） */
    fun speak(text: String) {
        if (!isInitialized) {
            Logger.w("TtsSpeaker: not initialized")
            return
        }
        Logger.i("TtsSpeaker: speaking '$text'")
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "voice_rider_tts")
    }

    /** 播报并等待完成，供语音流程使用 */
    suspend fun speakAndWait(text: String) {
        if (!isInitialized) {
            Logger.w("TtsSpeaker: not initialized")
            return
        }
        val t = tts ?: return

        suspendCancellableCoroutine { cont ->
            val done = AtomicBoolean(false)
            t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    if (!done.getAndSet(true)) cont.resume(Unit)
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    if (!done.getAndSet(true)) cont.resume(Unit)
                }
                override fun onError(utteranceId: String?, errorCode: Int) {
                    if (!done.getAndSet(true)) cont.resume(Unit)
                }
                override fun onStop(utteranceId: String?, interrupted: Boolean) {
                    if (!done.getAndSet(true)) cont.resume(Unit)
                }
            })
            Logger.i("TtsSpeaker: speaking+wait '$text'")
            val ret = t.speak(text, TextToSpeech.QUEUE_FLUSH, null, "voice_rider_tts")
            if (ret != TextToSpeech.SUCCESS) {
                t.setOnUtteranceProgressListener(null)
                cont.resume(Unit)
            }
        }
    }

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
    }
}

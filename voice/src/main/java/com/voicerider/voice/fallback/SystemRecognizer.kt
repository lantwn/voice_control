package com.voicerider.voice.fallback

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.voicerider.core.util.Logger
import com.voicerider.voice.config.VoiceConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import java.util.concurrent.atomic.AtomicBoolean

class SystemRecognizer(private val context: Context) {
    private var recognizer: SpeechRecognizer? = null

    suspend fun recognize(): String? = withTimeoutOrNull(VoiceConfig.LISTEN_TIMEOUT_MS) {
        suspendCancellableCoroutine { cont ->
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                Logger.w("SystemRecognizer: not available")
                cont.resume(null)
                return@suspendCancellableCoroutine
            }

            // 防止 onResults/onError 同时触发导致 double-resume 崩溃
            val isResumed = AtomicBoolean(false)

            recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : android.speech.RecognitionListener {
                    override fun onResults(results: android.os.Bundle?) {
                        if (!isResumed.compareAndSet(false, true)) return
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        cont.resume(matches?.firstOrNull())
                        recognizer?.destroy()
                        recognizer = null
                    }
                    override fun onError(error: Int) {
                        if (!isResumed.compareAndSet(false, true)) return
                        Logger.w("SystemRecognizer: error code $error")
                        cont.resume(null)
                        recognizer?.destroy()
                        recognizer = null
                    }
                    override fun onReadyForSpeech(params: android.os.Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onPartialResults(partialResults: android.os.Bundle?) {}
                    override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
                })
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "zh-CN")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }
                startListening(intent)
            }

            cont.invokeOnCancellation {
                if (isResumed.compareAndSet(false, true)) {
                    recognizer?.destroy()
                    recognizer = null
                }
            }
        }
    }

    fun destroy() {
        recognizer?.destroy()
        recognizer = null
    }
}

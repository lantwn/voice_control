package com.voicerider.voice.engine

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.iflytek.cloud.ErrorCode
import com.iflytek.cloud.InitListener
import com.iflytek.cloud.RecognizerListener
import com.iflytek.cloud.RecognizerResult
import com.iflytek.cloud.SpeechConstant
import com.iflytek.cloud.SpeechError
import com.iflytek.cloud.SpeechRecognizer
import com.voicerider.core.util.Logger
import com.voicerider.voice.config.VoiceConfig
import com.voicerider.voice.fallback.SystemRecognizer
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

class CommandRecognizer(
    private val context: Context,
    private val systemRecognizer: SystemRecognizer
) {
    private var recognizer: SpeechRecognizer? = null
    private var useXunfeiOnline = true

    suspend fun recognize(): String? {
        val rawText = if (useXunfeiOnline) {
            recognizeViaXunfei() ?: fallbackToSystem()
        } else {
            fallbackToSystem()
        }

        if (rawText.isNullOrBlank()) {
            Logger.w("CommandRecognizer: no speech recognized")
            return null
        }

        Logger.i("CommandRecognizer: recognized '$rawText'")
        return rawText
    }

    private suspend fun recognizeViaXunfei(): String? = suspendCancellableCoroutine { cont ->
        val r = SpeechRecognizer.createRecognizer(context, InitListener { code ->
            if (code != ErrorCode.SUCCESS) {
                Logger.w("CommandRecognizer: ASR init failed, code=$code")
            }
        })

        if (r == null) {
            Logger.w("CommandRecognizer: createRecognizer returned null")
            cont.resume(null)
            return@suspendCancellableCoroutine
        }

        recognizer = r
        val isResumed = AtomicBoolean(false)
        val resultBuilder = StringBuilder()
        val handler = Handler(Looper.getMainLooper())

        val timeoutTask = Runnable {
            if (!isResumed.get()) {
                Logger.i("CommandRecognizer: timeout ${VoiceConfig.LISTEN_TIMEOUT_MS}ms, stopping ASR")
                r.stopListening()
            }
        }
        handler.postDelayed(timeoutTask, VoiceConfig.LISTEN_TIMEOUT_MS)

        r.setParameter(SpeechConstant.PARAMS, null)
        r.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD)
        r.setParameter(SpeechConstant.RESULT_TYPE, "json")
        r.setParameter(SpeechConstant.LANGUAGE, "zh_cn")
        r.setParameter(SpeechConstant.ACCENT, "mandarin")
        r.setParameter(SpeechConstant.VAD_BOS, "4000")
        r.setParameter(SpeechConstant.VAD_EOS, "1500")
        r.setParameter(SpeechConstant.ASR_PTT, "1")

        val ret = r.startListening(object : RecognizerListener {
            override fun onResult(results: RecognizerResult, isLast: Boolean) {
                val text = parseResultJson(results.resultString)
                resultBuilder.append(text)
                Logger.i("CommandRecognizer: onResult '$text' isLast=$isLast")
                if (isLast && !isResumed.getAndSet(true)) {
                    handler.removeCallbacks(timeoutTask)
                    val finalText = resultBuilder.toString().trim()
                    cont.resume(finalText.ifEmpty { null })
                }
            }

            override fun onError(error: SpeechError) {
                Logger.w("CommandRecognizer: onError code=${error.errorCode}, ${error.getPlainDescription(true)}")
                if (!isResumed.getAndSet(true)) {
                    handler.removeCallbacks(timeoutTask)
                    val text = resultBuilder.toString().trim()
                    cont.resume(text.ifEmpty { null })
                }
            }

            override fun onEndOfSpeech() {
                Logger.i("CommandRecognizer: onEndOfSpeech")
            }

            override fun onBeginOfSpeech() {
                Logger.i("CommandRecognizer: onBeginOfSpeech")
            }

            override fun onVolumeChanged(volume: Int, data: ByteArray?) {}

            override fun onEvent(eventType: Int, arg1: Int, arg2: Int, obj: android.os.Bundle?) {}
        })

        if (ret != ErrorCode.SUCCESS) {
            Logger.w("CommandRecognizer: startListening failed, code=$ret")
            handler.removeCallbacks(timeoutTask)
            isResumed.set(true)
            cont.resume(null)
        }

        cont.invokeOnCancellation {
            if (isResumed.compareAndSet(false, true)) {
                handler.removeCallbacks(timeoutTask)
                r.cancel()
            }
        }
    }

    private suspend fun fallbackToSystem(): String? {
        Logger.d("CommandRecognizer: falling back to system recognizer")
        useXunfeiOnline = false
        return systemRecognizer.recognize()
    }

    fun setOnlineMode(online: Boolean) {
        useXunfeiOnline = online
    }

    fun destroy() {
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
        systemRecognizer.destroy()
    }

    companion object {
        /** 用 org.json 正确解析讯飞 IAT 返回的 JSON */
        fun parseResultJson(json: String): String {
            val sb = StringBuilder()
            try {
                val root = JSONObject(json)
                val ws = root.optJSONArray("ws") ?: return ""
                for (i in 0 until ws.length()) {
                    val cw = ws.optJSONObject(i)?.optJSONArray("cw") ?: continue
                    for (j in 0 until cw.length()) {
                        val w = cw.optJSONObject(j)?.optString("w") ?: ""
                        sb.append(w)
                    }
                }
            } catch (e: Exception) {
                Logger.w("CommandRecognizer: JSON parse failed: ${e.message}")
            }
            return sb.toString()
        }
    }
}

package com.voicerider.voice.engine

import com.voicerider.core.util.Logger
import com.voicerider.voice.config.VoiceConfig
import com.voicerider.voice.fallback.SystemRecognizer

class CommandRecognizer(private val systemRecognizer: SystemRecognizer) {
    private var useXunfeiOnline = true

    /**
     * 识别语音并返回原始文本。
     * @return 识别到的原始文本，超时或识别失败返回 null。
     */
    suspend fun recognize(): String? {
        val rawText = if (useXunfeiOnline) {
            recognizeViaXunfei() ?: fallbackToSystem()
        } else {
            fallbackToSystem()
        }

        if (rawText.isNullOrBlank()) {
            Logger.w("CommandRecognizer: no speech recognized (timeout or empty)")
            return null
        }

        Logger.i("CommandRecognizer: recognized '$rawText'")
        return rawText
    }

    private suspend fun recognizeViaXunfei(): String? {
        Logger.d("CommandRecognizer: Xunfei online ASR (stub)")
        return null
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
        systemRecognizer.destroy()
    }
}

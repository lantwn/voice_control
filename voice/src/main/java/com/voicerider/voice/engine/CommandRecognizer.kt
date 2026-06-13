package com.voicerider.voice.engine

import com.voicerider.core.model.CommandType
import com.voicerider.core.model.VoiceCommand
import com.voicerider.core.util.Logger
import com.voicerider.voice.config.VoiceConfig
import com.voicerider.voice.fallback.SystemRecognizer

class CommandRecognizer(private val systemRecognizer: SystemRecognizer) {
    private var useXunfeiOnline = true

    suspend fun recognize(): VoiceCommand? {
        val rawText = if (useXunfeiOnline) {
            recognizeViaXunfei() ?: fallbackToSystem()
        } else {
            fallbackToSystem()
        } ?: return null

        val commandType = CommandType.fromText(rawText)
        if (commandType == null) {
            Logger.w("CommandRecognizer: no match for '$rawText'")
            return null
        }

        Logger.i("CommandRecognizer: matched ${commandType.name} from '$rawText'")
        return VoiceCommand(
            type = commandType,
            rawText = rawText,
            confidence = 0.8f
        )
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

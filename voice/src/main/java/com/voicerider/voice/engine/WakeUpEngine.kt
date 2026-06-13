package com.voicerider.voice.engine

import com.voicerider.core.util.Logger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class WakeUpEngine {
    private val _wakeEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val wakeEvents: SharedFlow<Unit> = _wakeEvents

    private var isInitialized = false

    fun initialize(wakeWord: String = "美团精灵"): Boolean {
        Logger.i("WakeUpEngine: initializing with wake word '$wakeWord'")
        // TODO: Replace with Xunfei SpeechWakeup SDK init
        isInitialized = true
        Logger.i("WakeUpEngine: initialized (stub — awaiting SDK)")
        return true
    }

    fun startListening() {
        if (!isInitialized) return
        Logger.d("WakeUpEngine: started listening")
        // TODO: Xunfei SpeechWakeup.startListening()
    }

    fun stopListening() {
        Logger.d("WakeUpEngine: stopped listening")
        // TODO: Xunfei SpeechWakeup.stopListening()
    }

    fun destroy() {
        Logger.d("WakeUpEngine: destroyed")
        // TODO: Xunfei SpeechWakeup.destroy()
    }
}

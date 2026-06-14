package com.voicerider.voice.config

object VoiceConfig {
    const val WAKE_WORD = "美团精灵"
    const val LISTEN_TIMEOUT_MS = 10_000L
    const val CONFIDENCE_THRESHOLD = 0.5f

    // Xunfei SDK — fill after registration
    var XUNFEI_APP_ID = "3c377751"
    var XUNFEI_API_KEY = "113b4662e592a8f02ec300cdfb52b3b3"
    var XUNFEI_API_SECRET = "ZWIxNjk2YjAzN2VmYWQwYTk2YzhlMWJk"

    // TTS
    const val TTS_SPEED = 50
    const val TTS_PITCH = 50
    const val TTS_VOLUME = 100
}

package com.voicerider.core.model

data class VoiceCommand(
    val type: CommandType,
    val rawText: String,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis()
)

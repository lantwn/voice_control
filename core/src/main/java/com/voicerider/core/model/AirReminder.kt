package com.voicerider.core.model

data class AirReminder(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: ReminderType,
    val level: ReminderLevel,
    val title: String,
    val message: String,
    val orderId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

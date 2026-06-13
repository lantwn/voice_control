package com.voicerider.core.model

enum class ReminderLevel(val priority: Int) {
    URGENT(0),
    IMPORTANT(1),
    INFO(2),
    SUMMARY(3);
}

package com.voicerider.core.util

import android.content.Context
import android.content.SharedPreferences

class PrefsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("voice_rider_prefs", Context.MODE_PRIVATE)

    var wakeWord: String
        get() = prefs.getString("wake_word", "美团精灵") ?: "美团精灵"
        set(value) = prefs.edit().putString("wake_word", value).apply()

    var isVoiceEnabled: Boolean
        get() = prefs.getBoolean("voice_enabled", true)
        set(value) = prefs.edit().putBoolean("voice_enabled", value).apply()

    var isAccessibilityEnabled: Boolean
        get() = prefs.getBoolean("accessibility_enabled", false)
        set(value) = prefs.edit().putBoolean("accessibility_enabled", value).apply()

    var navMode: String
        get() = prefs.getString("nav_mode", "BIKE") ?: "BIKE"
        set(value) = prefs.edit().putString("nav_mode", value).apply()

    var elementMappingVersion: String
        get() = prefs.getString("element_mapping_version", "1.0.0") ?: "1.0.0"
        set(value) = prefs.edit().putString("element_mapping_version", value).apply()

    fun getString(key: String, default: String = ""): String =
        prefs.getString(key, default) ?: default

    fun putString(key: String, value: String) =
        prefs.edit().putString(key, value).apply()
}

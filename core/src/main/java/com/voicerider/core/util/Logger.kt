package com.voicerider.core.util

import android.util.Log

object Logger {
    private const val TAG = "VoiceRider"
    private var isDebug = true

    fun d(message: String) { if (isDebug) Log.d(TAG, message) }
    fun i(message: String) { Log.i(TAG, message) }
    fun w(message: String) { Log.w(TAG, message) }
    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }
}

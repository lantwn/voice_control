package com.voicerider.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import com.voicerider.app.R
import com.voicerider.core.util.Logger

class FloatingWindowService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var isListening = false

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createFloatingWindow()
        startForeground()
    }

    private fun createFloatingWindow() {
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        floatingView = inflater.inflate(R.layout.view_floating_window, null)

        val params = WindowManager.LayoutParams(
            56.dpToPx(),
            56.dpToPx(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 300
        }

        floatingView?.setOnClickListener {
            Logger.d("FloatingWindow: clicked — triggering voice recognition")
            toggleListening()
            VoiceRoutingService.instance?.startVoiceRecognition()
        }

        floatingView?.setOnLongClickListener {
            Logger.d("FloatingWindow: long clicked — stopping self")
            stopSelf()
            true
        }

        windowManager?.addView(floatingView, params)
    }

    fun toggleListening() {
        isListening = !isListening
        if (isListening) {
            floatingView?.startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.pulse_scale)
            )
        } else {
            floatingView?.clearAnimation()
        }
    }

    private fun startForeground() {
        val channelId = "voice_rider_floating"
        if (Build.VERSION_CODES.O <= Build.VERSION.SDK_INT) {
            val channel = NotificationChannel(
                channelId, "悬浮窗", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Voice Rider")
            .setContentText("悬浮窗运行中")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()
        startForeground(1002, notification)
    }

    private fun Int.dpToPx(): Int =
        (this * resources.displayMetrics.density).toInt()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        floatingView?.let { windowManager?.removeView(it) }
        super.onDestroy()
    }
}

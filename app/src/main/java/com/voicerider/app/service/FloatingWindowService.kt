package com.voicerider.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import com.voicerider.app.R
import com.voicerider.core.util.Logger

class FloatingWindowService : Service() {

    enum class WindowState { IDLE, LISTENING, SUCCESS, ERROR }

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var currentState = WindowState.IDLE
    private val handler = Handler(Looper.getMainLooper())

    // Dragging support
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var hasDragged = false
    private var layoutParams: WindowManager.LayoutParams? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createFloatingWindow()
        startForeground()
    }

    private fun createFloatingWindow() {
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        floatingView = inflater.inflate(R.layout.view_floating_window, null)

        layoutParams = WindowManager.LayoutParams(
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
            setState(WindowState.LISTENING)
            VoiceRoutingService.instance?.startVoiceRecognition()
        }

        floatingView?.setOnLongClickListener {
            Logger.d("FloatingWindow: long clicked — returning to main app")
            val intent = Intent(this, com.voicerider.app.ui.MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            true
        }

        // Enable dragging (short tap = click, drag beyond threshold = move)
        floatingView?.setOnTouchListener { view, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams?.x ?: 0
                    initialY = layoutParams?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    hasDragged = false
                    false // let system see the event for click detection
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (kotlin.math.abs(dx) > 10 || kotlin.math.abs(dy) > 10) {
                        hasDragged = true
                        layoutParams?.x = initialX + dx.toInt()
                        layoutParams?.y = initialY + dy.toInt()
                        layoutParams?.let { windowManager?.updateViewLayout(view, it) }
                    }
                    hasDragged
                }
                else -> false
            }
        }

        windowManager?.addView(floatingView, layoutParams)
        applyStateVisuals()
    }

    fun setState(state: WindowState) {
        if (currentState == state) return
        currentState = state
        Logger.d("FloatingWindow: state → $state")
        applyStateVisuals()

        // Auto-reset SUCCESS/ERROR back to IDLE after 2s
        if (state == WindowState.SUCCESS || state == WindowState.ERROR) {
            handler.postDelayed({ setState(WindowState.IDLE) }, 2000)
        }
    }

    private fun applyStateVisuals() {
        val view = floatingView ?: return

        // Clear previous animations
        view.clearAnimation()

        when (currentState) {
            WindowState.IDLE -> {
                view.setBackgroundResource(R.drawable.bg_floating_idle)
            }
            WindowState.LISTENING -> {
                view.setBackgroundResource(R.drawable.bg_floating_listening)
                view.startAnimation(
                    AnimationUtils.loadAnimation(this, R.anim.pulse_scale)
                )
            }
            WindowState.SUCCESS -> {
                view.setBackgroundResource(R.drawable.bg_floating_success)
                view.startAnimation(
                    AnimationUtils.loadAnimation(this, R.anim.bounce_in)
                )
            }
            WindowState.ERROR -> {
                view.setBackgroundResource(R.drawable.bg_floating_error)
                view.startAnimation(
                    AnimationUtils.loadAnimation(this, R.anim.shake)
                )
            }
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
        instance = null
        floatingView?.let { windowManager?.removeView(it) }
        super.onDestroy()
    }

    companion object {
        var instance: FloatingWindowService? = null
            private set
    }
}

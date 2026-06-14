package com.voicerider.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.voicerider.core.model.CommandType
import com.voicerider.core.model.VoiceCommand
import com.voicerider.core.util.Logger
import com.voicerider.voice.engine.CommandRecognizer
import com.voicerider.voice.engine.TtsSpeaker
import com.voicerider.voice.engine.WakeUpEngine
import com.voicerider.voice.fallback.SystemRecognizer

class VoiceRoutingService : Service() {

    private lateinit var wakeUpEngine: WakeUpEngine
    private lateinit var commandRecognizer: CommandRecognizer
    private lateinit var ttsSpeaker: TtsSpeaker

    private var onCommand: ((VoiceCommand) -> Unit)? = null
    private var onFeedback: ((String, Boolean) -> Unit)? = null

    override fun onCreate() {
        super.onCreate()
        Logger.i("VoiceRoutingService: created")

        wakeUpEngine = WakeUpEngine()
        ttsSpeaker = TtsSpeaker(this)
        commandRecognizer = CommandRecognizer(SystemRecognizer(this))

        wakeUpEngine.initialize()
        ttsSpeaker.initialize()

        startForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        wakeUpEngine.startListening()
        return START_STICKY
    }

    fun setOnCommandListener(listener: (VoiceCommand) -> Unit) {
        onCommand = listener
    }

    fun setOnFeedbackListener(listener: (String, Boolean) -> Unit) {
        onFeedback = listener
    }

    fun handleTextCommand(text: String) {
        val commandType = CommandType.fromText(text)
        if (commandType != null) {
            val cmd = VoiceCommand(type = commandType, rawText = text, confidence = 1.0f)
            Logger.i("VoiceRoutingService: text command matched ${commandType.name}")

            // TTS 语音反馈
            ttsSpeaker.speak(commandType.feedback)

            // UI 视觉反馈
            onFeedback?.invoke(commandType.feedback, true)

            onCommand?.invoke(cmd)
        } else {
            ttsSpeaker.speak("未识别的指令")
            onFeedback?.invoke("未识别的指令", false)
        }
    }

    fun speakFeedback(message: String) {
        ttsSpeaker.speak(message)
    }

    private fun startForeground() {
        val channelId = "voice_rider_voice"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "语音服务", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Voice Rider")
            .setContentText("语音服务运行中")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()
        startForeground(1001, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        wakeUpEngine.destroy()
        commandRecognizer.destroy()
        ttsSpeaker.destroy()
        super.onDestroy()
    }
}

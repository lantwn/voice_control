package com.voicerider.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.session.MediaSession
import android.os.Build
import android.os.IBinder
import android.view.KeyEvent
import com.voicerider.core.model.CommandType
import com.voicerider.core.model.VoiceCommand
import com.voicerider.core.util.Logger
import com.voicerider.voice.engine.CommandRecognizer
import com.voicerider.voice.engine.TtsSpeaker
import com.voicerider.voice.engine.WakeUpEngine
import com.voicerider.voice.fallback.SystemRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class VoiceRoutingService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var wakeUpEngine: WakeUpEngine
    private lateinit var commandRecognizer: CommandRecognizer
    private lateinit var ttsSpeaker: TtsSpeaker

    private var onCommand: ((VoiceCommand) -> Unit)? = null
    private var onFeedback: ((String, Boolean) -> Unit)? = null

    // 蓝牙耳机按键 → MediaSession
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        Logger.i("VoiceRoutingService: created")
        instance = this

        wakeUpEngine = WakeUpEngine()
        ttsSpeaker = TtsSpeaker(this)
        commandRecognizer = CommandRecognizer(SystemRecognizer(this))

        wakeUpEngine.initialize()
        ttsSpeaker.initialize()
        setupMediaSession()

        // Collect wake word events → start voice recognition
        serviceScope.launch {
            wakeUpEngine.wakeEvents.collect {
                Logger.i("VoiceRoutingService: wake word detected")
                ttsSpeaker.speak("请说")
                val rawText = commandRecognizer.recognize()
                when {
                    // 超时：5 秒内无语音输入
                    rawText == null -> {
                        ttsSpeaker.speak("请再说一次")
                        onFeedback?.invoke("请再说一次", false)
                    }
                    // 识别到语音
                    else -> recognizeAndDispatch(rawText)
                }
            }
        }

        // 在 collector 启动后立即开始监听，避免丢失早期唤醒事件
        wakeUpEngine.startListening()

        startForeground()
    }

    /**
     * 注册 MediaSession 捕获蓝牙耳机通话键作为语音触发入口。
     * 设计 5.1 节 — 蓝牙按键激活方式。
     */
    private fun setupMediaSession() {
        mediaSession = MediaSession(this, "VoiceRiderBluetooth").apply {
            setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
            setCallback(object : MediaSession.Callback() {
                override fun onMediaButtonEvent(mediaButtonIntent: Intent?): Boolean {
                    val event: KeyEvent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        mediaButtonIntent?.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        mediaButtonIntent?.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
                    }
                    if (event == null) return false
                    if (event.action == KeyEvent.ACTION_DOWN &&
                        (event.keyCode == KeyEvent.KEYCODE_HEADSETHOOK ||
                         event.keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                    ) {
                        Logger.i("VoiceRoutingService: Bluetooth button pressed")
                        serviceScope.launch { onBluetoothTrigger() }
                        return true
                    }
                    return false
                }
            })
            isActive = true
        }
    }

    /** 蓝牙按键触发语音识别 */
    private suspend fun onBluetoothTrigger() {
        wakeUpEngine.stopListening()
        ttsSpeaker.speak("请说")
        val rawText = commandRecognizer.recognize()
        when {
            rawText == null -> {
                ttsSpeaker.speak("请再说一次")
                onFeedback?.invoke("请再说一次", false)
            }
            else -> recognizeAndDispatch(rawText)
        }
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

    /** Called from UI (text input or floating window tap) */
    fun handleTextCommand(text: String) {
        val commandType = CommandType.fromText(text)
        if (commandType != null) {
            val cmd = VoiceCommand(type = commandType, rawText = text, confidence = 1.0f)
            dispatchCommand(cmd)
        } else {
            ttsSpeaker.speak("未识别的指令")
            onFeedback?.invoke("未识别的指令", false)
        }
    }

    /** Called from floating window tap to start listening */
    fun startVoiceRecognition() {
        wakeUpEngine.stopListening()
        ttsSpeaker.speak("请说")
        serviceScope.launch {
            val rawText = commandRecognizer.recognize()
            when {
                rawText == null -> {
                    ttsSpeaker.speak("请再说一次")
                    onFeedback?.invoke("请再说一次", false)
                }
                else -> recognizeAndDispatch(rawText)
            }
        }
    }

    fun speakFeedback(message: String) {
        ttsSpeaker.speak(message)
    }

    /** 命令匹配 + 分发 */
    private fun recognizeAndDispatch(rawText: String) {
        val commandType = CommandType.fromText(rawText)
        if (commandType != null) {
            val cmd = VoiceCommand(type = commandType, rawText = rawText, confidence = 0.8f)
            dispatchCommand(cmd)
        } else {
            Logger.w("VoiceRoutingService: no command match for '$rawText'")
            ttsSpeaker.speak("未识别的指令")
            onFeedback?.invoke("未识别的指令", false)
        }
    }

    private fun dispatchCommand(cmd: VoiceCommand) {
        Logger.i("VoiceRoutingService: dispatching ${cmd.type.name} — \"${cmd.rawText}\"")
        // TTS 语音反馈
        ttsSpeaker.speak(cmd.type.feedback)
        // UI 视觉反馈
        onFeedback?.invoke(cmd.type.feedback, true)
        // 命令回调（→ accessibility 自动化），失败时回调会触发 speakFeedback
        onCommand?.invoke(cmd)
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
        instance = null
        serviceScope.cancel()
        mediaSession?.release()
        wakeUpEngine.destroy()
        commandRecognizer.destroy()
        ttsSpeaker.destroy()
        super.onDestroy()
    }

    companion object {
        var instance: VoiceRoutingService? = null
            private set
    }
}

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
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        Logger.i("VoiceRoutingService: created")
        instance = this

        wakeUpEngine = WakeUpEngine(this)
        ttsSpeaker = TtsSpeaker(this)
        commandRecognizer = CommandRecognizer(this, SystemRecognizer(this))

        wakeUpEngine.initialize()
        ttsSpeaker.initialize()
        setupMediaSession()

        // 唤醒词 → 语音识别 → 指令执行
        serviceScope.launch {
            wakeUpEngine.wakeEvents.collect {
                Logger.i("VoiceRoutingService: wake word detected")
                wakeUpEngine.stopListening()

                // 1. 播报提示音，等待播放完毕（避免 TTS 音频进入麦克风）
                ttsSpeaker.speakAndWait("请说")

                // 2. 启动 ASR 识别指令
                val rawText = commandRecognizer.recognize()
                if (rawText != null) {
                    recognizeAndDispatch(rawText)
                } else {
                    Logger.w("VoiceRoutingService: ASR returned null")
                    ttsSpeaker.speak("请再说一次")
                    onFeedback?.invoke("未识别到语音", false)
                }

                // 3. 恢复唤醒监听
                wakeUpEngine.startListening()
            }
        }

        wakeUpEngine.startListening()
        startForeground()
    }

    private fun setupMediaSession() {
        mediaSession = MediaSession(this, "VoiceRiderBluetooth").apply {
            setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
            setCallback(object : MediaSession.Callback() {
                override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
                    val event: KeyEvent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
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

    private suspend fun onBluetoothTrigger() {
        wakeUpEngine.stopListening()
        ttsSpeaker.speakAndWait("请说")
        val rawText = commandRecognizer.recognize()
        if (rawText != null) recognizeAndDispatch(rawText)
        else {
            ttsSpeaker.speak("请再说一次")
            onFeedback?.invoke("未识别到语音", false)
        }
        wakeUpEngine.startListening()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        wakeUpEngine.startListening()
        return START_STICKY
    }

    fun setOnCommandListener(listener: (VoiceCommand) -> Unit) { onCommand = listener }
    fun setOnFeedbackListener(listener: (String, Boolean) -> Unit) { onFeedback = listener }

    fun handleTextCommand(text: String) {
        val commandType = CommandType.fromText(text)
        if (commandType != null) {
            dispatchCommand(VoiceCommand(type = commandType, rawText = text, confidence = 1.0f))
        } else {
            ttsSpeaker.speak("未识别的指令")
            onFeedback?.invoke("未识别的指令", false)
        }
    }

    fun startVoiceRecognition() {
        wakeUpEngine.stopListening()
        serviceScope.launch {
            ttsSpeaker.speakAndWait("请说")
            val rawText = commandRecognizer.recognize()
            if (rawText != null) recognizeAndDispatch(rawText)
            else {
                ttsSpeaker.speak("请再说一次")
                onFeedback?.invoke("未识别到语音", false)
            }
            wakeUpEngine.startListening()
        }
    }

    fun speakFeedback(message: String) = ttsSpeaker.speak(message)

    private fun recognizeAndDispatch(rawText: String) {
        val commandType = CommandType.fromText(rawText)
        if (commandType != null) {
            dispatchCommand(VoiceCommand(type = commandType, rawText = rawText, confidence = 0.8f))
        } else {
            Logger.w("VoiceRoutingService: no command match for '$rawText'")
            ttsSpeaker.speak("未识别的指令")
            onFeedback?.invoke("未识别的指令：$rawText", false)
        }
    }

    private fun dispatchCommand(cmd: VoiceCommand) {
        Logger.i("VoiceRoutingService: dispatching ${cmd.type.name} — \"${cmd.rawText}\"")
        ttsSpeaker.speak(cmd.type.feedback)
        onFeedback?.invoke(cmd.type.feedback, true)
        onCommand?.invoke(cmd)
    }

    private fun startForeground() {
        val channelId = "voice_rider_voice"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "语音服务", NotificationManager.IMPORTANCE_LOW)
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

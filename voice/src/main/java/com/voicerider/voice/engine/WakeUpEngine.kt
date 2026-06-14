package com.voicerider.voice.engine

import android.content.Context
import com.iflytek.cloud.SpeechConstant
import com.iflytek.cloud.SpeechError
import com.iflytek.cloud.VoiceWakeuper
import com.iflytek.cloud.WakeuperListener
import com.iflytek.cloud.WakeuperResult
import com.iflytek.cloud.util.ResourceUtil
import com.voicerider.core.util.Logger
import com.voicerider.voice.config.VoiceConfig
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class WakeUpEngine(private val context: Context) {
    private val _wakeEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val wakeEvents: SharedFlow<Unit> = _wakeEvents

    private var wakeuper: VoiceWakeuper? = null
    private var isInitialized = false
    private var isListening = false

    fun initialize(): Boolean {
        if (isInitialized) return true

        wakeuper = VoiceWakeuper.createWakeuper(context, null)
        if (wakeuper == null) {
            Logger.e("WakeUpEngine: createWakeuper failed — check libmsc.so and SpeechUtility init")
            return false
        }

        isInitialized = true
        Logger.i("WakeUpEngine: initialized, wake word='${VoiceConfig.WAKE_WORD}'")
        return true
    }

    fun startListening() {
        if (!isInitialized) {
            Logger.w("WakeUpEngine: not initialized")
            return
        }

        val w = wakeuper ?: return

        // 清空参数
        w.setParameter(SpeechConstant.PARAMS, null)

        // 唤醒门限值 (0-3000, 越低越灵敏)
        w.setParameter(SpeechConstant.IVW_THRESHOLD, "0:1450")

        // 纯唤醒模式
        w.setParameter(SpeechConstant.IVW_SST, "wakeup")

        // 持续唤醒
        w.setParameter(SpeechConstant.KEEP_ALIVE, "1")

        // 唤醒资源路径
        val resPath = ResourceUtil.generateResourcePath(
            context, ResourceUtil.RESOURCE_TYPE.assets,
            "ivw/${VoiceConfig.XUNFEI_APP_ID}.jet"
        )
        w.setParameter(ResourceUtil.IVW_RES_PATH, resPath)

        // 音频保存
        w.setParameter(SpeechConstant.AUDIO_FORMAT, "wav")
        w.setParameter(
            SpeechConstant.IVW_AUDIO_PATH,
            context.getExternalFilesDir("msc")?.absolutePath + "/ivw.wav"
        )

        val ret = w.startListening(wakeuperListener)
        if (ret != 0) {
            Logger.e("WakeUpEngine: startListening failed, error=$ret")
            return
        }

        isListening = true
        Logger.i("WakeUpEngine: listening for '${VoiceConfig.WAKE_WORD}'")
    }

    fun stopListening() {
        wakeuper?.stopListening()
        isListening = false
    }

    fun destroy() {
        stopListening()
        wakeuper?.destroy()
        wakeuper = null
        isInitialized = false
    }

    private val wakeuperListener = object : WakeuperListener {
        override fun onResult(result: WakeuperResult) {
            Logger.i("WakeUpEngine: wake word detected → ${result.resultString}")
            _wakeEvents.tryEmit(Unit)
        }

        override fun onError(error: SpeechError) {
            Logger.w("WakeUpEngine: error code=${error.errorCode}, ${error.getPlainDescription(true)}")
        }

        override fun onBeginOfSpeech() {
            Logger.d("WakeUpEngine: begin of speech")
        }

        override fun onEvent(eventType: Int, isLast: Int, arg2: Int, obj: android.os.Bundle?) {}

        override fun onVolumeChanged(volume: Int) {}
    }
}

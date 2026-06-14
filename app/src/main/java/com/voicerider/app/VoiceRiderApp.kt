package com.voicerider.app

import android.app.Application
import com.iflytek.cloud.SpeechConstant
import com.iflytek.cloud.SpeechUtility
import com.voicerider.core.util.Logger
import com.voicerider.voice.config.VoiceConfig

class VoiceRiderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initXunfei()
    }

    private fun initXunfei() {
        val param = "appid=${VoiceConfig.XUNFEI_APP_ID},${SpeechConstant.ENGINE_MODE}=${SpeechConstant.MODE_MSC}"
        SpeechUtility.createUtility(this, param)
        Logger.i("VoiceRiderApp: iFlytek SDK initialized, appid=${VoiceConfig.XUNFEI_APP_ID}")
    }
}

package com.voicerider.accessibility.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.voicerider.accessibility.automator.MeituanAutomator
import com.voicerider.accessibility.automator.OrderActionHandler
import com.voicerider.core.config.AppConfig
import com.voicerider.core.util.Logger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class RiderAccessibilityService : AccessibilityService() {

    private val automator = MeituanAutomator()
    private val handler = OrderActionHandler(automator)

    private val _screenRoot = MutableSharedFlow<AccessibilityNodeInfo>(
        extraBufferCapacity = 1
    )
    val screenRoot: SharedFlow<AccessibilityNodeInfo> = _screenRoot

    val orderActionHandler: OrderActionHandler get() = handler

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        if (packageName != AppConfig.MEITUAN_RIDER_PACKAGE) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                rootInActiveWindow?.let { _screenRoot.tryEmit(it) }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Logger.i("RiderAccessibilityService: connected")
    }

    override fun onInterrupt() {
        Logger.w("RiderAccessibilityService: interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.i("RiderAccessibilityService: destroyed")
    }
}

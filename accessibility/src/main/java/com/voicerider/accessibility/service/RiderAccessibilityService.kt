package com.voicerider.accessibility.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.voicerider.accessibility.automator.ActionResult
import com.voicerider.accessibility.automator.MeituanAutomator
import com.voicerider.accessibility.automator.OrderActionHandler
import com.voicerider.core.config.AppConfig
import com.voicerider.core.model.VoiceCommand
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

    /** Cached latest root for synchronous command handling */
    @Volatile
    private var latestRoot: AccessibilityNodeInfo? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        if (packageName != AppConfig.MEITUAN_RIDER_PACKAGE) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                rootInActiveWindow?.let {
                    _screenRoot.tryEmit(it)
                    latestRoot = it
                }
            }
        }
    }

    /**
     * Called by [com.voicerider.app.service.VoiceRoutingService] via the
     * ViewModel bridge to execute a voice command against the current screen.
     */
    fun handleVoiceCommand(command: VoiceCommand): ActionResult {
        val root = latestRoot
        if (root == null) {
            Logger.w("RiderAccessibilityService: no screen root available")
            return ActionResult(
                success = false,
                message = "无法获取美团界面，请打开美团骑手APP",
                requiresManual = true
            )
        }
        return handler.handleCommand(command, root)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Logger.i("RiderAccessibilityService: connected")
        instance = this
    }

    override fun onInterrupt() {
        Logger.w("RiderAccessibilityService: interrupted")
        latestRoot = null
    }

    override fun onDestroy() {
        instance = null
        latestRoot = null
        super.onDestroy()
        Logger.i("RiderAccessibilityService: destroyed")
    }

    companion object {
        var instance: RiderAccessibilityService? = null
            private set
    }
}

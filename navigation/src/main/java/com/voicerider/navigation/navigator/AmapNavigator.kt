package com.voicerider.navigation.navigator

import com.voicerider.core.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class NavState { IDLE, PLANNING, NAVIGATING, ARRIVED, ERROR }

class AmapNavigator {
    private val _navState = MutableStateFlow(NavState.IDLE)
    val navState: StateFlow<NavState> = _navState

    var currentDistance: Float = 0f
    var currentDuration: Int = 0

    fun startNavigation() {
        Logger.i("AmapNavigator: starting navigation")
        _navState.value = NavState.NAVIGATING
        // TODO: Replace with AMap Navi SDK
        // AMapNavi.startNavi(NaviType.GPS)
    }

    fun stopNavigation() {
        Logger.i("AmapNavigator: stopping navigation")
        _navState.value = NavState.IDLE
        // TODO: AMapNavi.stopNavi()
    }

    fun getRemainingInfo(): String {
        return "剩余 ${currentDistance.toInt()} 米，约 ${currentDuration / 60} 分钟"
    }
}

package com.voicerider.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.voicerider.app.service.VoiceRoutingService
import com.voicerider.core.model.AirReminder
import com.voicerider.core.model.Order
import com.voicerider.core.model.OrderStatus
import com.voicerider.core.model.ReminderLevel
import com.voicerider.core.model.ReminderType

class HomeViewModel : ViewModel() {
    private val _orders = MutableLiveData<List<Order>>(emptyList())
    val orders: LiveData<List<Order>> = _orders

    private val _reminders = MutableLiveData<List<AirReminder>>(emptyList())
    val reminders: LiveData<List<AirReminder>> = _reminders

    private val _remindersPreview = MutableLiveData<List<AirReminder>>(emptyList())
    val remindersPreview: LiveData<List<AirReminder>> = _remindersPreview

    private val _todayCompleted = MutableLiveData(0)
    val todayCompleted: LiveData<Int> = _todayCompleted

    private val _todayInProgress = MutableLiveData(0)
    val todayInProgress: LiveData<Int> = _todayInProgress

    private val _todayIncome = MutableLiveData(0.0)
    val todayIncome: LiveData<Double> = _todayIncome

    private val _isVoiceReady = MutableLiveData(true)
    val isVoiceReady: LiveData<Boolean> = _isVoiceReady

    private val _commandFeedback = MutableLiveData<Pair<String, Boolean>>()
    val commandFeedback: LiveData<Pair<String, Boolean>> = _commandFeedback

    private val _navigateToOrderDetail = MutableLiveData<Order>()
    val navigateToOrderDetail: LiveData<Order> = _navigateToOrderDetail

    init {
        loadMockData()
    }

    fun wireVoiceService() {
        VoiceRoutingService.instance?.apply {
            // UI 反馈监听
            setOnFeedbackListener { message, success ->
                _commandFeedback.postValue(Pair(message, success))
                // 同步悬浮窗状态
                val floatState = if (success)
                    com.voicerider.app.service.FloatingWindowService.WindowState.SUCCESS
                else
                    com.voicerider.app.service.FloatingWindowService.WindowState.ERROR
                com.voicerider.app.service.FloatingWindowService.instance?.setState(floatState)
            }
            // 命令 → Accessibility 自动化桥接
            setOnCommandListener { command ->
                val result = com.voicerider.accessibility.service.RiderAccessibilityService
                    .instance?.handleVoiceCommand(command)
                if (result != null) {
                    if (!result.success) {
                        // 操作失败 → TTS 播报错误原因
                        VoiceRoutingService.instance?.speakFeedback(result.message)
                        _commandFeedback.postValue(Pair(result.message, false))
                    }
                    // else: 成功由 onFeedback 回调处理（dispatchCommand 中已触发）
                }
            }
        }
    }

    fun onOrderClicked(order: Order) {
        _navigateToOrderDetail.postValue(order)
    }

    fun onVoiceInput(text: String) {
        VoiceRoutingService.instance?.handleTextCommand(text)
    }

    private fun loadMockData() {
        _todayCompleted.value = 8
        _todayInProgress.value = 2
        _todayIncome.value = 186.0

        _orders.value = listOf(
            Order(
                id = "20240612038",
                status = OrderStatus.ACCEPTED,
                merchantName = "肯德基",
                merchantAddress = "万达店",
                customerName = "张先生",
                customerAddress = "万达广场3号楼1203",
                customerPhone = "138****5678",
                amount = 32.5,
                distanceKm = 2.3f
            ),
            Order(
                id = "20240612037",
                status = OrderStatus.DELIVERING,
                merchantName = "麦当劳",
                merchantAddress = "银泰店",
                customerName = "李女士",
                customerAddress = "银泰城A座508",
                customerPhone = "139****1234",
                amount = 28.0,
                distanceKm = 1.8f
            )
        )

        val allReminders = listOf(
            AirReminder(
                type = ReminderType.CUSTOMER_URGE,
                level = ReminderLevel.URGENT,
                title = "#038 顾客催单",
                message = "顾客发消息：\"到哪了？\"",
                orderId = "20240612038"
            ),
            AirReminder(
                type = ReminderType.PICKUP_REMINDER,
                level = ReminderLevel.IMPORTANT,
                title = "#037 取餐提醒",
                message = "预计出餐时间已到，请尽快取餐",
                orderId = "20240612037"
            ),
            AirReminder(
                type = ReminderType.ROUTE_SUGGESTION,
                level = ReminderLevel.INFO,
                title = "#038 路线建议",
                message = "南门进电梯更快到3号楼",
                orderId = "20240612038"
            )
        )
        _reminders.value = allReminders
        _remindersPreview.value = allReminders.take(3)
    }
}

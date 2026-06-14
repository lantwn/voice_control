package com.voicerider.accessibility.automator

import android.view.accessibility.AccessibilityNodeInfo
import com.voicerider.core.model.CommandType
import com.voicerider.core.model.Order
import com.voicerider.core.model.OrderStatus
import com.voicerider.core.model.VoiceCommand
import com.voicerider.core.util.Logger

class OrderActionHandler(
    private val automator: MeituanAutomator
) {
    private var currentOrder: Order? = null

    fun handleCommand(
        command: VoiceCommand,
        root: AccessibilityNodeInfo
    ): ActionResult {
        val order = currentOrder

        // Validate state
        command.type.requiredStatus?.let { required ->
            if (order?.status != required) {
                Logger.w("OrderActionHandler: state mismatch — need $required, have ${order?.status}")
                return ActionResult(
                    success = false,
                    message = "当前不是${required.label}状态，无法执行${command.rawText}",
                    requiresManual = false
                )
            }
        }

        val success = when (command.type) {
            CommandType.ACCEPT_ORDER -> automator.clickAcceptButton(root)
            CommandType.REJECT_ORDER -> automator.clickRejectButton(root)
            CommandType.PICKUP_DONE -> automator.clickPickupDone(root)
            CommandType.DELIVERY_DONE -> automator.clickDeliveryDone(root)
            else -> false // CALL/SMS handled by :app
        }

        if (!success) {
            return ActionResult(
                success = false,
                message = "${command.rawText}操作失败，请手动点击",
                requiresManual = true
            )
        }

        updateOrderState(command.type)
        return ActionResult(
            success = true,
            message = "${command.rawText}成功",
            requiresManual = false
        )
    }

    fun updateCurrentOrder(order: Order) {
        currentOrder = order
        Logger.i("OrderActionHandler: current order = ${order.id} (${order.status.label})")
    }

    private fun updateOrderState(commandType: CommandType) {
        currentOrder = currentOrder?.copy(
            status = when (commandType) {
                CommandType.ACCEPT_ORDER -> OrderStatus.ACCEPTED
                CommandType.PICKUP_DONE -> OrderStatus.PICKED_UP
                CommandType.DELIVERY_DONE -> OrderStatus.COMPLETED
                else -> currentOrder?.status ?: OrderStatus.WAITING
            }
        )
    }
}

data class ActionResult(
    val success: Boolean,
    val message: String,
    val requiresManual: Boolean
)

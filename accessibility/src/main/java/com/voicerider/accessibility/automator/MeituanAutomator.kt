package com.voicerider.accessibility.automator

import android.view.accessibility.AccessibilityNodeInfo
import com.voicerider.accessibility.strategy.ElementLocator
import com.voicerider.accessibility.strategy.ElementTarget
import com.voicerider.core.config.AppConfig
import com.voicerider.core.util.Logger

class MeituanAutomator {

    fun clickAcceptButton(root: AccessibilityNodeInfo): Boolean {
        Logger.i("MeituanAutomator: clicking accept button")
        return clickWithRetry(root, ElementTarget(
            resourceId = "${AppConfig.MEITUAN_RIDER_PACKAGE}:id/btn_accept",
            text = "抢单"
        ))
    }

    fun clickRejectButton(root: AccessibilityNodeInfo): Boolean {
        return clickWithRetry(root, ElementTarget(
            text = "拒单"
        ))
    }

    fun clickPickupDone(root: AccessibilityNodeInfo): Boolean {
        return clickWithRetry(root, ElementTarget(
            resourceId = "${AppConfig.MEITUAN_RIDER_PACKAGE}:id/btn_pickup_done",
            text = "确认取餐"
        ))
    }

    fun clickDeliveryDone(root: AccessibilityNodeInfo): Boolean {
        return clickWithRetry(root, ElementTarget(
            resourceId = "${AppConfig.MEITUAN_RIDER_PACKAGE}:id/btn_delivery_done",
            text = "确认送达"
        ))
    }

    fun extractOrderInfo(root: AccessibilityNodeInfo): Map<String, String> {
        val info = mutableMapOf<String, String>()
        info["merchant"] = ElementLocator.extractText(root, ElementTarget(
            resourceId = "${AppConfig.MEITUAN_RIDER_PACKAGE}:id/tv_merchant_name"
        )) ?: ""
        info["customer_address"] = ElementLocator.extractText(root, ElementTarget(
            resourceId = "${AppConfig.MEITUAN_RIDER_PACKAGE}:id/tv_customer_address"
        )) ?: ""
        info["amount"] = ElementLocator.extractText(root, ElementTarget(
            resourceId = "${AppConfig.MEITUAN_RIDER_PACKAGE}:id/tv_amount"
        )) ?: ""
        Logger.d("MeituanAutomator: extracted order info — $info")
        return info
    }

    fun extractAddress(root: AccessibilityNodeInfo): String? {
        return ElementLocator.extractText(root, ElementTarget(
            resourceId = "${AppConfig.MEITUAN_RIDER_PACKAGE}:id/tv_address"
        )) ?: ElementLocator.extractText(root, ElementTarget(
            text = "地址"
        ))
    }

    private fun clickWithRetry(root: AccessibilityNodeInfo, target: ElementTarget): Boolean {
        var retryCount = 0
        while (retryCount < AppConfig.MAX_AUTOMATION_RETRIES) {
            if (ElementLocator.findAndClick(root, target)) return true
            retryCount++
            Logger.w("MeituanAutomator: retry $retryCount/${AppConfig.MAX_AUTOMATION_RETRIES}")
            if (retryCount < AppConfig.MAX_AUTOMATION_RETRIES) {
                Thread.sleep(100) // brief delay before next attempt
            }
        }
        return false
    }
}

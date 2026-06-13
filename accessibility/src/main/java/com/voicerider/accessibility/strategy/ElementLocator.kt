package com.voicerider.accessibility.strategy

import android.view.accessibility.AccessibilityNodeInfo
import com.voicerider.core.util.Logger

enum class LocateStrategy { RESOURCE_ID, TEXT, CONTENT_DESC }

data class ElementTarget(
    val resourceId: String? = null,
    val text: String? = null,
    val contentDesc: String? = null
)

object ElementLocator {

    fun findAndClick(
        root: AccessibilityNodeInfo,
        target: ElementTarget
    ): Boolean {
        val node = findNode(root, target)
        if (node != null) {
            val clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Logger.i("ElementLocator: clicked ${target.text ?: target.resourceId} → $clicked")
            return clicked
        }
        Logger.w("ElementLocator: not found → ${target.text ?: target.resourceId}")
        return false
    }

    fun extractText(
        root: AccessibilityNodeInfo,
        target: ElementTarget
    ): String? {
        val node = findNode(root, target)
        return node?.text?.toString()?.trim()
    }

    fun findNode(root: AccessibilityNodeInfo, target: ElementTarget): AccessibilityNodeInfo? {
        // Layer 1: Resource ID
        target.resourceId?.let { id ->
            root.findAccessibilityNodeInfosByViewId(id)?.firstOrNull()?.let {
                Logger.d("ElementLocator: found by resource-id '$id'")
                return it
            }
        }

        // Layer 2: Text match
        target.text?.let { text ->
            val found = findByText(root, text)
            if (found != null) {
                Logger.d("ElementLocator: found by text '$text'")
                return found
            }
        }

        // Layer 3: Content description
        target.contentDesc?.let { desc ->
            val found = findByContentDesc(root, desc)
            if (found != null) {
                Logger.d("ElementLocator: found by content-desc '$desc'")
                return found
            }
        }

        return null
    }

    private fun findByText(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        if (node.text?.toString()?.contains(text) == true && node.isClickable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findByText(child, text)
            if (found != null) return found
        }
        return null
    }

    private fun findByContentDesc(node: AccessibilityNodeInfo, desc: String): AccessibilityNodeInfo? {
        if (node.contentDescription?.toString()?.contains(desc) == true && node.isClickable)
            return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findByContentDesc(child, desc)
            if (found != null) return found
        }
        return null
    }
}

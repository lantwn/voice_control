package com.voicerider.accessibility.strategy

import android.view.accessibility.AccessibilityNodeInfo
import com.voicerider.core.util.Logger
import java.util.ArrayDeque

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
        return if (node != null) {
            val clickTarget = findClickableOrSelf(node)
            val clicked = clickTarget.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Logger.i("ElementLocator: clicked ${target.text ?: target.resourceId} → $clicked")
            clickTarget.recycle()
            if (clickTarget !== node) node.recycle()
            clicked
        } else {
            Logger.w("ElementLocator: not found → ${target.text ?: target.resourceId}")
            false
        }
    }

    fun extractText(
        root: AccessibilityNodeInfo,
        target: ElementTarget
    ): String? {
        val node = findNode(root, target)
        val result = node?.text?.toString()?.trim()
        node?.recycle()
        return result
    }

    fun findNode(root: AccessibilityNodeInfo, target: ElementTarget): AccessibilityNodeInfo? {
        // Layer 1: Resource ID
        target.resourceId?.let { id ->
            root.findAccessibilityNodeInfosByViewId(id)?.firstOrNull()?.let {
                Logger.d("ElementLocator: found by resource-id '$id'")
                return it
            }
        }

        // Layer 2: Text match (iterative BFS, no stack overflow)
        target.text?.let { text ->
            val found = bfsSearch(root) { node ->
                node.text?.toString()?.contains(text) == true
            }
            if (found != null) {
                Logger.d("ElementLocator: found by text '$text'")
                return found
            }
        }

        // Layer 3: Content description
        target.contentDesc?.let { desc ->
            val found = bfsSearch(root) { node ->
                node.contentDescription?.toString()?.contains(desc) == true
            }
            if (found != null) {
                Logger.d("ElementLocator: found by content-desc '$desc'")
                return found
            }
        }

        return null
    }

    /** 迭代 BFS 遍历 UI 树，防止深层嵌套导致的 StackOverflow */
    private fun bfsSearch(
        root: AccessibilityNodeInfo,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.addLast(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (predicate(node)) {
                // 匹配成功 — 回收队列中剩余节点（由 getChild() 获取，需手动回收）
                for (remaining in queue) { remaining.recycle() }
                return node
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.addLast(it) }
            }
        }
        return null
    }

    /**
     * 如果找到的节点本身不可点击，向上查找最近的可点击祖先。
     * 解决 TextView 包裹在 Button 内的情况。
     */
    private fun findClickableOrSelf(node: AccessibilityNodeInfo): AccessibilityNodeInfo {
        if (node.isClickable) return node
        var parent: AccessibilityNodeInfo? = node.parent
        var prev: AccessibilityNodeInfo? = null
        while (parent != null) {
            if (parent.isClickable) {
                prev?.recycle()
                return parent
            }
            val next = parent.parent
            prev?.recycle()
            prev = parent
            parent = next
        }
        prev?.recycle()
        // 没有可点击祖先，尝试点击原节点（某些 View 无需 isClickable 也可响应）
        return node
    }

    /**
     * 如果找到的节点本身不可点击，向上查找最近的可点击祖先。
     * 解决 TextView 包裹在 Button 内的情况。
     */
    private fun findClickableOrSelf(node: AccessibilityNodeInfo): AccessibilityNodeInfo {
        if (node.isClickable) return node
        var parent = node.parent
        while (parent != null) {
            if (parent.isClickable) return parent
            val next = parent.parent
            if (next == null && parent !== node) parent.recycle()
            parent = next
        }
        // 没有可点击祖先，尝试点击原节点（某些 View 无需 isClickable 也可响应）
        return node
    }
}

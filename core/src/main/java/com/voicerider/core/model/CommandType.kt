package com.voicerider.core.model

enum class CommandType(
    val patterns: List<String>,
    val requiredStatus: OrderStatus?,
    val feedback: String  // TTS + UI 反馈提示
) {
    ACCEPT_ORDER(listOf("接单", "抢单", "抢"), OrderStatus.WAITING, "已接单"),
    REJECT_ORDER(listOf("不接", "拒单", "拒"), OrderStatus.WAITING, "已拒单"),
    PICKUP_DONE(listOf("已取餐", "取货完成", "取餐了"), OrderStatus.ACCEPTED, "已确认取餐"),
    DELIVERY_DONE(listOf("已送达", "送到了", "到了"), OrderStatus.PICKED_UP, "已确认送达"),
    NAV_TO_MERCHANT(listOf("导航到取餐点", "去取餐", "导航取餐", "取餐导航"), OrderStatus.ACCEPTED, "正在规划取餐路线"),
    NAV_TO_CUSTOMER(listOf("导航到顾客", "导航送餐", "送餐导航", "去送餐"), OrderStatus.PICKED_UP, "正在规划送餐路线"),
    CALL_CUSTOMER(listOf("打电话给顾客", "打电话", "联系顾客"), null, "正在拨打顾客电话"),
    SEND_MESSAGE(listOf("发消息", "发短信", "给顾客发消息"), null, "已打开短信"),
    QUERY_ORDER(listOf("查看订单", "订单详情", "还剩几单", "什么单"), null, "正在查询订单");

    /** 简单包含匹配，依赖 [fromText] 中按长度降序排序 + 否定检测来防误匹配 */
    fun matches(text: String): Boolean =
        patterns.any { text.contains(it) }

    companion object {
        /**
         * 否定前缀 — 用于排除"不想接单"→误匹配为接单的情况。
         * 只使用多字否定词，避免单字"不"在常见词中误匹配（如"不错""是不是"）。
         */
        private val NEGATION_WORDS = listOf("不想", "不要", "不用", "别接", "别抢", "拒绝", "不接", "不抢", "不导航", "不取", "不送")

        fun fromText(text: String): CommandType? {
            // 按 pattern 长度降序匹配，优先匹配更精确的长命令
            val sorted = entries.sortedByDescending { it.patterns.maxOfOrNull { p -> p.length } ?: 0 }
            for (cmd in sorted) {
                if (cmd.matches(text)) {
                    // REJECT_ORDER 本身就是否定命令，不额外做否定检测（避免"不接"阻断自身）
                    if (cmd != REJECT_ORDER && hasNegation(text)) continue
                    return cmd
                }
            }
            return null
        }

        /** 文本是否包含否定意图（"不想"、"不要"、"别"、"拒绝"等） */
        private fun hasNegation(text: String): Boolean =
            NEGATION_WORDS.any { text.contains(it) }
    }
}

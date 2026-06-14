package com.voicerider.core.model

enum class CommandType(
    val patterns: List<String>,
    val requiredStatus: OrderStatus?,
    val feedback: String  // TTS + UI 反馈提示
) {
    ACCEPT_ORDER(listOf("接单", "抢", "接"), OrderStatus.WAITING, "已接单"),
    REJECT_ORDER(listOf("不接", "拒单", "拒"), OrderStatus.WAITING, "已拒单"),
    PICKUP_DONE(listOf("已取餐", "取货完成", "取餐了"), OrderStatus.ACCEPTED, "已确认取餐"),
    DELIVERY_DONE(listOf("已送达", "送到了", "到了"), OrderStatus.DELIVERING, "已确认送达"),
    CALL_CUSTOMER(listOf("打电话给顾客", "打电话", "联系顾客"), null, "正在拨打顾客电话"),
    SEND_MESSAGE(listOf("发消息", "发短信", "给顾客发消息"), null, "已打开短信"),
    QUERY_ORDER(listOf("查看订单", "订单详情", "还剩几单", "什么单"), null, "正在查询订单");

    fun matches(text: String): Boolean =
        patterns.any { text.contains(it) }

    companion object {
        fun fromText(text: String): CommandType? =
            entries.firstOrNull { it.matches(text) }
    }
}

package com.voicerider.core.model

enum class CommandType(
    val patterns: List<String>,
    val requiredStatus: OrderStatus?
) {
    ACCEPT_ORDER(listOf("接单", "抢", "接"), OrderStatus.WAITING),
    REJECT_ORDER(listOf("不接", "拒单", "拒"), OrderStatus.WAITING),
    PICKUP_DONE(listOf("已取餐", "取货完成", "取餐了"), OrderStatus.ACCEPTED),
    DELIVERY_DONE(listOf("已送达", "送到了", "到了"), OrderStatus.DELIVERING),
    NAV_TO_PICKUP(listOf("导航到取餐点", "去取餐", "取餐导航"), null),
    NAV_TO_CUSTOMER(listOf("导航到顾客", "去送货", "送餐导航"), null),
    CALL_CUSTOMER(listOf("打电话给顾客", "打电话", "联系顾客"), null),
    SEND_MESSAGE(listOf("发消息", "发短信", "给顾客发消息"), null),
    QUERY_ORDER(listOf("查看订单", "订单详情", "还剩几单", "什么单"), null);

    fun matches(text: String): Boolean =
        patterns.any { text.contains(it) }

    companion object {
        fun fromText(text: String): CommandType? =
            entries.firstOrNull { it.matches(text) }
    }
}

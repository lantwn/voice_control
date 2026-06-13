package com.voicerider.core.model

enum class OrderStatus(val label: String) {
    WAITING("抢单大厅"),
    ACCEPTED("已接单"),
    PICKED_UP("已取餐"),
    DELIVERING("配送中"),
    COMPLETED("已送达");
}

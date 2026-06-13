package com.voicerider.core.model

enum class ReminderType(val label: String) {
    CUSTOMER_URGE("顾客催单"),
    TIMEOUT_WARNING("即将超时"),
    STATUS_CHANGE("状态变更"),
    PICKUP_REMINDER("取餐提醒"),
    ROUTE_SUGGESTION("路线建议"),
    ADDRESS_CHANGE("地址变更"),
    INCOME_SUMMARY("收入统计");
}

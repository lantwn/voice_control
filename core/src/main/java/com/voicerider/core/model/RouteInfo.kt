package com.voicerider.core.model

/**
 * 路线信息 — 对应设计文档 Section 8
 */
data class RouteInfo(
    val fromLat: Double,
    val fromLng: Double,
    val toLat: Double,
    val toLng: Double,
    val distance: Float,   // 米
    val duration: Int,     // 秒
    val polyline: String? = null  // 高德 polyline 编码
)

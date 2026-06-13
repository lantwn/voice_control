package com.voicerider.core.model

data class RouteInfo(
    val fromName: String,
    val toName: String,
    val fromLat: Double,
    val fromLng: Double,
    val toLat: Double,
    val toLng: Double,
    val distanceMeters: Float,
    val durationSeconds: Int
)

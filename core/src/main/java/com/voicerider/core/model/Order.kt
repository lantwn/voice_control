package com.voicerider.core.model

data class Order(
    val id: String,
    val status: OrderStatus,
    val merchantName: String,
    val merchantAddress: String,
    val customerName: String,
    val customerAddress: String,
    val customerPhone: String,
    val amount: Double,
    val distanceKm: Float,
    val acceptedTime: Long = System.currentTimeMillis(),
    val estimatedDelivery: Long = System.currentTimeMillis() + 1800_000
)

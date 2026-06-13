package com.voicerider.core.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CommandTypeTest {

    @Test
    fun `match accept order from text`() {
        assertEquals(CommandType.ACCEPT_ORDER, CommandType.fromText("接单"))
        assertEquals(CommandType.ACCEPT_ORDER, CommandType.fromText("抢"))
    }

    @Test
    fun `match pickup done from text`() {
        assertEquals(CommandType.PICKUP_DONE, CommandType.fromText("已取餐"))
        assertEquals(CommandType.PICKUP_DONE, CommandType.fromText("取货完成"))
    }

    @Test
    fun `match delivery done from text`() {
        assertEquals(CommandType.DELIVERY_DONE, CommandType.fromText("已送达"))
        assertEquals(CommandType.DELIVERY_DONE, CommandType.fromText("到了"))
    }

    @Test
    fun `return null for unrecognized text`() {
        assertNull(CommandType.fromText("随便说点什么"))
    }

    @Test
    fun `accept order requires waiting status`() {
        assertEquals(OrderStatus.WAITING, CommandType.ACCEPT_ORDER.requiredStatus)
    }

    @Test
    fun `delivery done requires delivering status`() {
        assertEquals(OrderStatus.DELIVERING, CommandType.DELIVERY_DONE.requiredStatus)
    }

    @Test
    fun `nav commands have no required status`() {
        assertNull(CommandType.NAV_TO_PICKUP.requiredStatus)
        assertNull(CommandType.NAV_TO_CUSTOMER.requiredStatus)
    }
}

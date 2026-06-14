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
    fun `delivery done requires PICKED_UP status`() {
        assertEquals(OrderStatus.PICKED_UP, CommandType.DELIVERY_DONE.requiredStatus)
    }

    @Test
    fun `call customer has no required status`() {
        assertNull(CommandType.CALL_CUSTOMER.requiredStatus)
    }

    // ========== 导航命令 ==========

    @Test
    fun `match nav to merchant from text`() {
        assertEquals(CommandType.NAV_TO_MERCHANT, CommandType.fromText("导航到取餐点"))
        assertEquals(CommandType.NAV_TO_MERCHANT, CommandType.fromText("去取餐"))
    }

    @Test
    fun `match nav to customer from text`() {
        assertEquals(CommandType.NAV_TO_CUSTOMER, CommandType.fromText("导航到顾客"))
        assertEquals(CommandType.NAV_TO_CUSTOMER, CommandType.fromText("送餐导航"))
    }

    @Test
    fun `nav to merchant requires ACCEPTED status`() {
        assertEquals(OrderStatus.ACCEPTED, CommandType.NAV_TO_MERCHANT.requiredStatus)
    }

    @Test
    fun `nav to customer requires PICKED_UP status`() {
        assertEquals(OrderStatus.PICKED_UP, CommandType.NAV_TO_CUSTOMER.requiredStatus)
    }

    // ========== 否定检测 ==========

    @Test
    fun `negation — 不想接单 should not match ACCEPT_ORDER`() {
        assertNotEquals(CommandType.ACCEPT_ORDER, CommandType.fromText("不想接单"))
    }

    @Test
    fun `negation — 不要接 should not match ACCEPT_ORDER`() {
        assertNotEquals(CommandType.ACCEPT_ORDER, CommandType.fromText("不要接"))
    }

    @Test
    fun `negation — 不接 should match REJECT_ORDER`() {
        assertEquals(CommandType.REJECT_ORDER, CommandType.fromText("不接"))
    }

    @Test
    fun `negation — 不抢 should match REJECT_ORDER`() {
        assertEquals(CommandType.REJECT_ORDER, CommandType.fromText("不抢"))
    }

    @Test
    fun `negation — 不导航到取餐点 should not match NAV_TO_MERCHANT`() {
        assertNotEquals(CommandType.NAV_TO_MERCHANT, CommandType.fromText("不导航到取餐点"))
    }

    @Test
    fun `negation — 不错这个订单我接了 should be recognized`() {
        // "不错" contains "不" but is not a negation word in our list
        assertNotNull(CommandType.fromText("不错这个订单我接了"))
    }
}

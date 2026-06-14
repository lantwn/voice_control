package com.voicerider.core.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * 订单状态机逻辑测试 — 纯 Kotlin，不依赖 Android SDK。
 * 用 ./gradlew :core:test 运行。
 */
class OrderStateMachineTest {

    private val sampleOrder = Order(
        id = "001",
        status = OrderStatus.WAITING,
        merchantName = "肯德基",
        merchantAddress = "万达店",
        customerName = "张先生",
        customerAddress = "万达广场3号楼",
        customerPhone = "13800000000",
        amount = 32.5,
        distanceKm = 2.3f
    )

    // ========== 抢单场景 ==========

    @Test
    fun `抢单 — WAITING状态下允许接单`() {
        val cmd = CommandType.fromText("接单")
        assertEquals(CommandType.ACCEPT_ORDER, cmd)
        assertEquals(OrderStatus.WAITING, cmd?.requiredStatus)
        assertTrue(cmd?.requiredStatus == sampleOrder.status)
    }

    @Test
    fun `抢单 — ACCEPTED状态下不允许再接单`() {
        val order = sampleOrder.copy(status = OrderStatus.ACCEPTED)
        val cmd = CommandType.fromText("接单")
        assertNotEquals(order.status, cmd?.requiredStatus)
    }

    @Test
    fun `拒单 — WAITING状态下允许拒单`() {
        val cmd = CommandType.fromText("不接")
        assertEquals(CommandType.REJECT_ORDER, cmd)
        assertEquals(OrderStatus.WAITING, cmd?.requiredStatus)
    }

    // ========== 取餐场景 ==========

    @Test
    fun `取餐 — ACCEPTED状态下允许确认取餐`() {
        val order = sampleOrder.copy(status = OrderStatus.ACCEPTED)
        val cmd = CommandType.fromText("已取餐")
        assertEquals(CommandType.PICKUP_DONE, cmd)
        assertEquals(OrderStatus.ACCEPTED, cmd?.requiredStatus)
        assertTrue(cmd?.requiredStatus == order.status)
    }

    @Test
    fun `取餐 — WAITING状态下不允许取餐`() {
        val cmd = CommandType.fromText("已取餐")
        assertNotEquals(OrderStatus.WAITING, cmd?.requiredStatus)
    }

    // ========== 送达场景 ==========

    @Test
    fun `送达 — PICKED_UP状态下允许确认送达`() {
        val order = sampleOrder.copy(status = OrderStatus.PICKED_UP)
        val cmd = CommandType.fromText("已送达")
        assertEquals(CommandType.DELIVERY_DONE, cmd)
        assertEquals(OrderStatus.PICKED_UP, cmd?.requiredStatus)
        assertTrue(cmd?.requiredStatus == order.status)
    }

    @Test
    fun `送达 — ACCEPTED状态下不允许送达`() {
        val cmd = CommandType.fromText("已送达")
        assertEquals(OrderStatus.PICKED_UP, cmd?.requiredStatus)
        assertNotEquals(OrderStatus.ACCEPTED, cmd?.requiredStatus)
    }

    // ========== 状态流转完整性 ==========

    @Test
    fun `完整配送流程流转`() {
        var order = sampleOrder

        // 接单：WAITING → ACCEPTED
        order = order.copy(status = OrderStatus.ACCEPTED)
        assertEquals(OrderStatus.ACCEPTED, order.status)

        // 取餐：ACCEPTED → PICKED_UP（取餐后进入已取餐状态）
        order = order.copy(status = OrderStatus.PICKED_UP)
        assertEquals(OrderStatus.PICKED_UP, order.status)

        // 导航送餐：PICKED_UP → 启动导航（状态不变，导航由 :navigation 处理）
        val navCmd = CommandType.fromText("导航到顾客")
        assertEquals(CommandType.NAV_TO_CUSTOMER, navCmd)
        assertEquals(OrderStatus.PICKED_UP, navCmd?.requiredStatus)

        // 配送进行中（由外部状态变更或自动化操作触发）
        order = order.copy(status = OrderStatus.DELIVERING)
        assertEquals(OrderStatus.DELIVERING, order.status)

        // 送达确认：PICKED_UP → COMPLETED（或 DELIVERING → COMPLETED）
        order = order.copy(status = OrderStatus.COMPLETED)
        assertEquals(OrderStatus.COMPLETED, order.status)
    }

    // ========== 导航命令场景 ==========

    @Test
    fun `导航取餐 — ACCEPTED状态下允许`() {
        val cmd = CommandType.fromText("导航到取餐点")
        assertEquals(CommandType.NAV_TO_MERCHANT, cmd)
        assertEquals(OrderStatus.ACCEPTED, cmd?.requiredStatus)
    }

    @Test
    fun `导航送餐 — PICKED_UP状态下允许`() {
        val cmd = CommandType.fromText("导航到顾客")
        assertEquals(CommandType.NAV_TO_CUSTOMER, cmd)
        assertEquals(OrderStatus.PICKED_UP, cmd?.requiredStatus)
    }

    @Test
    fun `导航送餐 — ACCEPTED状态下不允许`() {
        val cmd = CommandType.fromText("导航送餐")
        assertEquals(OrderStatus.PICKED_UP, cmd?.requiredStatus)
        assertNotEquals(OrderStatus.ACCEPTED, cmd?.requiredStatus)
    }

    // ========== 模糊命令匹配 ==========

    @Test
    fun `命令匹配 — 容忍部分匹配`() {
        assertNotNull(CommandType.fromText("我要接单"))
        assertNotNull(CommandType.fromText("已经取餐了"))
        assertNotNull(CommandType.fromText("送到了啊"))
    }

    @Test
    fun `命令匹配 — 返回null对无效输入`() {
        assertNull(CommandType.fromText("今天天气不错"))
        assertNull(CommandType.fromText(""))
    }
}

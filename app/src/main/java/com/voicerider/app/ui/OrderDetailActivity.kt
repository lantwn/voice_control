package com.voicerider.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.voicerider.app.R
import com.voicerider.app.viewmodel.HomeViewModel
import com.voicerider.core.model.CommandType
import com.voicerider.core.model.OrderStatus

class OrderDetailActivity : AppCompatActivity() {

    private lateinit var viewModel: HomeViewModel
    private var orderId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_detail)

        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        orderId = intent.getStringExtra("order_id")

        val order = viewModel.orders.value?.find { it.id == orderId }
        if (order == null) {
            Toast.makeText(this, "订单不存在", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Toolbar
        findViewById<TextView>(R.id.tv_back).setOnClickListener { finish() }
        findViewById<TextView>(R.id.tv_toolbar_title).text = "#${order.id}"

        // Status tag
        val statusTag = findViewById<TextView>(R.id.tv_status_tag)
        statusTag.text = order.status.label
        statusTag.setBackgroundResource(when (order.status) {
            OrderStatus.WAITING -> R.drawable.bg_status_tag_waiting
            OrderStatus.ACCEPTED -> R.drawable.bg_status_tag_accepted
            OrderStatus.DELIVERING -> R.drawable.bg_status_tag_delivering
            else -> R.drawable.bg_status_tag
        })

        // Merchant
        findViewById<TextView>(R.id.tv_merchant_name).text = order.merchantName
        findViewById<TextView>(R.id.tv_merchant_address).text = "地址：${order.merchantAddress}"

        // Customer
        findViewById<TextView>(R.id.tv_customer_name).text = order.customerName
        findViewById<TextView>(R.id.tv_customer_address).text = "地址：${order.customerAddress}"
        findViewById<TextView>(R.id.tv_customer_phone).text = "电话：${order.customerPhone}"

        // Amount & Distance
        findViewById<TextView>(R.id.tv_amount).text = "¥${order.amount}"
        findViewById<TextView>(R.id.tv_distance).text = "${order.distanceKm}km"

        // Order ID footer
        findViewById<TextView>(R.id.tv_order_id).text = "订单号：${order.id}"

        // Action buttons
        findViewById<TextView>(R.id.btn_nav_merchant).setOnClickListener {
            viewModel.onVoiceInput("导航到取餐点")
            Toast.makeText(this, "已触发：导航到取餐点", Toast.LENGTH_SHORT).show()
        }

        findViewById<TextView>(R.id.btn_call_customer).setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${order.customerPhone.replace("*", "")}")
            }
            startActivity(intent)
        }

        findViewById<TextView>(R.id.btn_nav_customer).setOnClickListener {
            viewModel.onVoiceInput("导航到顾客")
            Toast.makeText(this, "已触发：导航送餐", Toast.LENGTH_SHORT).show()
        }

        // Bottom action button (context-sensitive)
        val btnAction = findViewById<TextView>(R.id.btn_action)
        when (order.status) {
            OrderStatus.ACCEPTED -> {
                btnAction.text = "确认取餐"
                btnAction.setOnClickListener {
                    viewModel.onVoiceInput("已取餐")
                    Toast.makeText(this, "已触发：确认取餐", Toast.LENGTH_SHORT).show()
                }
            }
            OrderStatus.PICKED_UP -> {
                btnAction.text = "确认送达"
                btnAction.setOnClickListener {
                    viewModel.onVoiceInput("已送达")
                    Toast.makeText(this, "已触发：确认送达", Toast.LENGTH_SHORT).show()
                }
            }
            OrderStatus.DELIVERING -> {
                btnAction.text = "确认送达"
                btnAction.setOnClickListener {
                    viewModel.onVoiceInput("已送达")
                    Toast.makeText(this, "已触发：确认送达", Toast.LENGTH_SHORT).show()
                }
            }
            OrderStatus.COMPLETED -> {
                btnAction.visibility = android.view.View.GONE
            }
            else -> {
                btnAction.text = "接单"
                btnAction.setOnClickListener {
                    viewModel.onVoiceInput("接单")
                    Toast.makeText(this, "已触发：接单", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

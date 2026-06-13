package com.voicerider.app.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.voicerider.app.R
import com.voicerider.core.model.Order
import com.voicerider.core.model.OrderStatus

class OrderListAdapter(
    private val onOrderClick: (Order) -> Unit
) : RecyclerView.Adapter<OrderViewHolder>() {

    private var orders: List<Order> = emptyList()

    fun submitList(list: List<Order>) {
        orders = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_card, parent, false)
        return OrderViewHolder(view, onOrderClick)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(orders[position])
    }

    override fun getItemCount() = orders.size
}

class OrderViewHolder(
    private val view: android.view.View,
    private val onOrderClick: (Order) -> Unit
) : RecyclerView.ViewHolder(view) {

    private val tvMerchant = view.findViewById<android.widget.TextView>(R.id.tv_merchant_name)
    private val tvStatus = view.findViewById<android.widget.TextView>(R.id.tv_status_tag)
    private val tvAmount = view.findViewById<android.widget.TextView>(R.id.tv_amount)
    private val tvDistance = view.findViewById<android.widget.TextView>(R.id.tv_distance)
    private val tvAddress = view.findViewById<android.widget.TextView>(R.id.tv_address)

    fun bind(order: Order) {
        tvMerchant.text = "${order.merchantName} ${order.merchantAddress}"

        tvStatus.apply {
            text = order.status.label
            when (order.status) {
                OrderStatus.ACCEPTED -> {
                    setTextColor(view.context.getColor(R.color.tag_waiting_text))
                    setBackgroundResource(R.drawable.bg_status_tag_waiting)
                }
                OrderStatus.DELIVERING -> {
                    setTextColor(view.context.getColor(R.color.tag_delivering_text))
                    setBackgroundResource(R.drawable.bg_status_tag_delivering)
                }
                else -> {
                    setTextColor(view.context.getColor(R.color.text_secondary))
                    setBackgroundResource(R.drawable.bg_status_tag)
                }
            }
        }

        tvAmount.text = "¥${order.amount}"
        tvDistance.text = "${order.distanceKm}km"
        tvAddress.text = "📍 ${order.customerAddress}"

        view.setOnClickListener { onOrderClick(order) }
    }
}

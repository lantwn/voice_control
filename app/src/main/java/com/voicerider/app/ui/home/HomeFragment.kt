package com.voicerider.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.voicerider.app.R
import com.voicerider.app.viewmodel.HomeViewModel

class HomeFragment : Fragment() {

    private lateinit var viewModel: HomeViewModel
    private lateinit var orderAdapter: OrderListAdapter
    private lateinit var reminderAdapter: AIReminderAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        setupOrders(view)
        setupReminders(view)
        setupVoiceBar(view)
        observeData()
    }

    private fun setupOrders(view: View) {
        orderAdapter = OrderListAdapter { order -> viewModel.onOrderClicked(order) }
        view.findViewById<RecyclerView>(R.id.rv_orders).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = orderAdapter
        }
    }

    private fun setupReminders(view: View) {
        reminderAdapter = AIReminderAdapter()
        view.findViewById<RecyclerView>(R.id.rv_reminders).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = reminderAdapter
        }
    }

    private fun setupVoiceBar(view: View) {
        view.findViewById<EditText>(R.id.et_command).setOnEditorActionListener { _, _, _ ->
            val text = view.findViewById<EditText>(R.id.et_command).text.toString()
            if (text.isNotBlank()) {
                viewModel.onVoiceInput(text)
                view.findViewById<EditText>(R.id.et_command).text?.clear()
                true
            } else false
        }
    }

    private fun observeData() {
        viewModel.orders.observe(viewLifecycleOwner) { orderAdapter.submitList(it) }
        viewModel.reminders.observe(viewLifecycleOwner) { reminders ->
            reminderAdapter.submitList(reminders)
            view?.findViewById<TextView>(R.id.tv_reminder_count)?.text = "${reminders.size}条"
        }
        viewModel.todayIncome.observe(viewLifecycleOwner) { income ->
            view?.findViewById<TextView>(R.id.tv_today_income)?.text =
                getString(R.string.label_today_income, income.toInt().toString())
        }
        viewModel.isVoiceReady.observe(viewLifecycleOwner) { ready ->
            val statusText = if (ready) "● 语音 · 无障碍 已开启" else "⚠ 服务未完全就绪"
            view?.findViewById<TextView>(R.id.tv_status_line)?.text = statusText
        }
        viewModel.commandFeedback.observe(viewLifecycleOwner) { (message, success) ->
            val emoji = if (success) "✅ " else "⚠️ "
            Toast.makeText(context, emoji + message, Toast.LENGTH_SHORT).show()
        }
    }
}

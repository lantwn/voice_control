package com.voicerider.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.voicerider.app.R
import com.voicerider.app.ui.home.AIReminderAdapter
import com.voicerider.app.viewmodel.HomeViewModel

class ReminderListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminder_list)

        val viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        findViewById<TextView>(R.id.tv_back).setOnClickListener { finish() }

        val adapter = AIReminderAdapter()
        val rv = findViewById<RecyclerView>(R.id.rv_reminders)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        val reminders = viewModel.reminders.value ?: emptyList()
        val emptyView = findViewById<View>(R.id.layout_empty)

        if (reminders.isEmpty()) {
            rv.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        } else {
            rv.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
            adapter.submitList(reminders)
        }

        findViewById<TextView>(R.id.tv_total_count).text = "共${reminders.size}条"
    }
}

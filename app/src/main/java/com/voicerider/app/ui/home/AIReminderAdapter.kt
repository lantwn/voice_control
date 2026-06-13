package com.voicerider.app.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import com.voicerider.app.R
import com.voicerider.core.model.AirReminder
import com.voicerider.core.model.ReminderLevel

class AIReminderAdapter :
    RecyclerView.Adapter<AIReminderViewHolder>() {

    private var reminders: List<AirReminder> = emptyList()

    fun submitList(list: List<AirReminder>) {
        reminders = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AIReminderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ai_reminder, parent, false)
        return AIReminderViewHolder(view)
    }

    override fun onBindViewHolder(holder: AIReminderViewHolder, position: Int) {
        holder.bind(reminders[position])
        holder.itemView.startAnimation(
            AnimationUtils.loadAnimation(holder.itemView.context, R.anim.slide_in_right)
        )
    }

    override fun getItemCount() = reminders.size
}

class AIReminderViewHolder(
    private val view: android.view.View
) : RecyclerView.ViewHolder(view) {

    private val dot = view.findViewById<android.view.View>(R.id.v_level_dot)
    private val title = view.findViewById<android.widget.TextView>(R.id.tv_reminder_title)
    private val time = view.findViewById<android.widget.TextView>(R.id.tv_reminder_time)

    fun bind(reminder: AirReminder) {
        dot.setBackgroundResource(when (reminder.level) {
            ReminderLevel.URGENT -> R.drawable.bg_dot_danger
            ReminderLevel.IMPORTANT -> R.drawable.bg_dot_warning
            ReminderLevel.INFO -> R.drawable.bg_dot_info
            ReminderLevel.SUMMARY -> R.drawable.bg_dot_success
        })

        title.text = "${reminder.title}\n${reminder.message}"
        time.text = "${reminder.timestamp}前"
    }
}

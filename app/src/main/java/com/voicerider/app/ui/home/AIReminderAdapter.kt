package com.voicerider.app.ui.home

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.voicerider.app.R
import com.voicerider.core.model.AirReminder
import com.voicerider.core.model.ReminderLevel

class AIReminderAdapter :
    ListAdapter<AirReminder, AIReminderViewHolder>(ReminderDiffCallback) {

    /** 仅在 item 首次 attach 时播放一次入场动画+震动 */
    private val attachedIds = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AIReminderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ai_reminder, parent, false)
        return AIReminderViewHolder(view)
    }

    override fun onBindViewHolder(holder: AIReminderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewAttachedToWindow(holder: AIReminderViewHolder) {
        super.onViewAttachedToWindow(holder)
        val pos = holder.bindingAdapterPosition
        if (pos == RecyclerView.NO_POSITION) return

        val reminder = getItem(pos)
        // 仅在首次 attach 时播放一次
        if (!attachedIds.add(reminder.id)) return

        // 入场动画（设计 6.3 节 — 右侧滑入 200ms）
        holder.itemView.startAnimation(
            AnimationUtils.loadAnimation(holder.itemView.context, R.anim.slide_in_right)
        )

        // 紧急级别：2 次触觉震动，仅一次（设计 6.3 节）
        if (reminder.level == ReminderLevel.URGENT) {
            triggerHapticFeedback(holder)
        }
    }

    override fun onViewDetachedFromWindow(holder: AIReminderViewHolder) {
        super.onViewDetachedFromWindow(holder)
        // 不清理 attachedIds — 生命周期内只播一次
    }

    /** 当列表清空时重置动画标记 */
    override fun onCurrentListChanged(
        previousList: List<AirReminder>,
        currentList: List<AirReminder>
    ) {
        super.onCurrentListChanged(previousList, currentList)
        if (currentList.isEmpty()) {
            attachedIds.clear()
        }
    }

    private fun triggerHapticFeedback(holder: AIReminderViewHolder) {
        val context = holder.itemView.context
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(VibratorManager::class.java)
            vibratorManager?.defaultVibrator?.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 100, 100, 100),
                    intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE),
                    -1
                )
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 100, 100, 100),
                    intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE),
                    -1
                )
            )
        }
    }

    private object ReminderDiffCallback : DiffUtil.ItemCallback<AirReminder>() {
        override fun areItemsTheSame(oldItem: AirReminder, newItem: AirReminder): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: AirReminder, newItem: AirReminder): Boolean =
            oldItem == newItem
    }
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

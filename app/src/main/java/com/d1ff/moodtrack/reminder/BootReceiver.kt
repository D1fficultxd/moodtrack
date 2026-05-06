package com.d1ff.moodtrack.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.d1ff.moodtrack.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val settingsRepo = SettingsRepository(context)
            CoroutineScope(Dispatchers.IO).launch {
                val isEnabled = settingsRepo.reminderEnabled.first()
                if (isEnabled) {
                    val hour = settingsRepo.reminderHour.first()
                    val minute = settingsRepo.reminderMinute.first()
                    ReminderManager.scheduleReminder(context, hour, minute)
                }
            }
        }
    }
}

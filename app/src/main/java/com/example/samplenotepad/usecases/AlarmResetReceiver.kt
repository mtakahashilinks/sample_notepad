package com.example.samplenotepad.usecases

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.samplenotepad.data.loadMemoInfoListWithReminderIO
import com.example.samplenotepad.data.resetAllAlarm

class AlarmResetReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        loadMemoInfoListWithReminderIO().resetAllAlarm(context)
    }
}

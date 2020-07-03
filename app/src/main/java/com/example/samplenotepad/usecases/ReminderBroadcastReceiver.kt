package com.example.samplenotepad.usecases

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.samplenotepad.entities.REQUEST_CODE_FOR_ALARM


class ReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val requestCode = intent?.getIntExtra(REQUEST_CODE_FOR_ALARM, -1)
    }
}
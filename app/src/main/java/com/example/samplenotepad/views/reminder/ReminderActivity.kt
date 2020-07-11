package com.example.samplenotepad.views.reminder

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import com.example.samplenotepad.R

class ReminderActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminder)
    }
}

//val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
//    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
//    putExtra(Settings.EXTRA_CHANNEL_ID, myNotificationChannel.getId())
//}
//startActivity(intent)

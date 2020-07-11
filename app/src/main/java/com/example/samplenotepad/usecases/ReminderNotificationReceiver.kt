package com.example.samplenotepad.usecases

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.samplenotepad.R
import com.example.samplenotepad.data.clearReminderValuesInMemoInfoIO
import com.example.samplenotepad.entities.*
import com.example.samplenotepad.views.SampleMemoApplication
import com.example.samplenotepad.views.reminder.ReminderActivity

class ReminderNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val requestCodeForAlarm = intent.getIntExtra(REQUEST_CODE_FOR_ALARM, -1)
        val memoTitle = intent.getStringExtra(MEMO_TITLE_FOR_ALARM)
            ?: context.resources.getString(R.string.memo_title_default_value)
        val alarmPosition = intent.getIntExtra(ALARM_POSITION, -1)
        val alarmType = requestCodeForAlarm % 10

        val notificationManager =
            context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        Log.d("場所:ReminderNotificationReceiver#onReceive", "requestCode=$requestCodeForAlarm alarmType=$alarmType")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = context.resources.getString(R.string.channel_name)
            val descriptionText = context.resources.getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val mChannel = NotificationChannel(CHANNEL_ID, channelName, importance).apply {
                description = descriptionText
            }

            notificationManager.createNotificationChannel(mChannel)
        }

        buildNotification(context, requestCodeForAlarm, memoTitle, alarmType, alarmPosition)

        //通知が全て終わったらデータベースのMemoInfoのReminder関係の値をclearする
        when (alarmType) {
            TARGET_DATE_TIME ->  {
                val intMemoInfoId = requestCodeForAlarm / 10
                val requestCodeForPostAlarm = intMemoInfoId * 10 + POST_ALARM
                val pendingIntent = requestCodeForPostAlarm.getPendingIntentForConfirmIFAlarmExist(
                    SampleMemoApplication.instance, memoTitle, alarmPosition
                )

                //postAlarmがセットされていなければ
                if (pendingIntent == null) intMemoInfoId.toLong().clearReminderValuesInMemoInfoIO()
            }
            POST_ALARM ->
                (requestCodeForAlarm / 10).toLong().clearReminderValuesInMemoInfoIO()
        }
    }


    private fun buildNotification(
        context: Context,
        notificationId: Int,
        notifyText: String,
        alarmType: Int,
        alarmPosition: Int
    ) {
        val notifyIntent = Intent(context, ReminderActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val notifyPendingIntent = PendingIntent.getActivity(
            context, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID).apply {
            val notifyTitle = when (alarmType) {
                TARGET_DATE_TIME -> context.resources.getString(R.string.notification_title_target)
                PRE_ALARM -> context.resources.getString(
                    R.string.notification_title_pre_post,
                    context.resources.getString(R.string.reminder_pre_alarm_label),
                    context.getTitleForPreAlarm(alarmPosition))
                else -> context.resources.getString(
                    R.string.notification_title_pre_post,
                    context.resources.getString(R.string.reminder_post_alarm_label),
                    context.getTitleForPostAlarm(alarmPosition))
            }

            priority = NotificationCompat.PRIORITY_MAX
            setSmallIcon(R.drawable.ic_alarm_black_24dp)
            setContentTitle(notifyTitle)
            setContentText(context.resources.getString(R.string.notification_text, notifyText))
            setContentIntent(notifyPendingIntent)
            setAutoCancel(true)
        }

        with(NotificationManagerCompat.from(context)) { notify(notificationId, builder.build()) }
    }

    private fun Context.getTitleForPreAlarm(position: Int): String = when (position) {
        PRE_POST_ALARM_5M -> this.resources.getString(R.string.pre_alarm_5m)
        PRE_POST_ALARM_10M -> this.resources.getString(R.string.pre_alarm_10m)
        PRE_POST_ALARM_30M -> this.resources.getString(R.string.pre_alarm_30m)
        PRE_POST_ALARM_1H -> this.resources.getString(R.string.pre_alarm_1h)
        else -> this.resources.getString(R.string.pre_alarm_24h)
    }

    private fun Context.getTitleForPostAlarm(position: Int): String = when (position) {
        PRE_POST_ALARM_5M -> this.resources.getString(R.string.post_alarm_5m)
        PRE_POST_ALARM_10M -> this.resources.getString(R.string.post_alarm_10m)
        PRE_POST_ALARM_30M -> this.resources.getString(R.string.post_alarm_30m)
        PRE_POST_ALARM_1H -> this.resources.getString(R.string.post_alarm_1h)
        else -> this.resources.getString(R.string.post_alarm_24h)
    }

    private fun RequestCode.getPendingIntentForConfirmIFAlarmExist(
        application: Application,
        memoTitle: String,
        alarmPosition: Int
    ): PendingIntent? {
        val intent = Intent(application.baseContext, ReminderNotificationReceiver::class.java)//.apply {
        //    putExtra(REQUEST_CODE_FOR_ALARM, this)
        //    putExtra(MEMO_TITLE_FOR_ALARM, memoTitle)
        //    putExtra(ALARM_POSITION, alarmPosition)
       // }

        return PendingIntent.getBroadcast(
            application.baseContext, this, intent, PendingIntent.FLAG_NO_CREATE
        )
    }
}

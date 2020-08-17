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
import com.example.samplenotepad.data.clearAllReminderStateInDataBaseIO
import com.example.samplenotepad.data.clearReminderStateInDatabaseIO
import com.example.samplenotepad.data.isAlarmExist
import com.example.samplenotepad.entities.*
import com.example.samplenotepad.views.display.MemoDisplayActivity

class ReminderNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val requestCodeForAlarm = intent.getIntExtra(ConstValForAlarm.REQUEST_CODE, -1)
        val memoTitle = intent.getStringExtra(ConstValForAlarm.MEMO_TITLE)
            ?: context.resources.getString(R.string.memo_title_default_value)
        val alarmPosition = intent.getIntExtra(ConstValForAlarm.ALARM_POSITION, -1)
        val alarmType = requestCodeForAlarm % 10
        val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        Log.d("場所:ReminderNotificationReceiver#onReceive", "requestCode=$requestCodeForAlarm alarmType=$alarmType")

        //SDKのVersionが26以上の場合は通知チャンネルが必要
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = context.resources.getString(R.string.channel_name)
            val descriptionText = context.resources.getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val mChannel = NotificationChannel(ConstValForAlarm.CHANNEL_ID, channelName, importance).apply {
                description = descriptionText
            }

            notificationManager.createNotificationChannel(mChannel)
        }

        buildNotification(
            context,
            requestCodeForAlarm / 10,
            requestCodeForAlarm,
            memoTitle,
            alarmType,
            alarmPosition
        )

        //通知が終わったらデータベースのReminder関係の値を初期化
        when (alarmType) {
            ConstValForAlarm.REMINDER_DATE_TIME ->  {
                val intMemoInfoId = requestCodeForAlarm / 10
                val requestCodeForPostAlarm = intMemoInfoId * 10 + ConstValForAlarm.POST_ALARM

                //PostAlarmがsetされているかの分岐
                when (requestCodeForPostAlarm.isAlarmExist(context) == null) {
                    true -> intMemoInfoId.toLong().clearAllReminderStateInDataBaseIO()
                    false -> clearReminderStateInDatabaseIO(
                        (requestCodeForAlarm / 10).toLong(), ConstValForAlarm.REMINDER_DATE_TIME
                    )
                }
            }
            ConstValForAlarm.PRE_ALARM -> clearReminderStateInDatabaseIO(
                (requestCodeForAlarm / 10).toLong(), ConstValForAlarm.PRE_ALARM
            )
            ConstValForAlarm.POST_ALARM ->
                (requestCodeForAlarm / 10).toLong().clearAllReminderStateInDataBaseIO()
        }
    }


    private fun buildNotification(
        context: Context,
        memoId: Int,
        notificationId: Int,
        notifyText: String,
        alarmType: Int,
        alarmPosition: Int
    ) {
        val notifyIntent = Intent(context, MemoDisplayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(ConstValForMemo.MEMO_Id, memoId.toLong())
        }
        val notifyPendingIntent = PendingIntent.getActivity(
            context, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )

        //SDKのVersionが25以下の場合はチャンネルIDは無視される
        val builder = NotificationCompat.Builder(context, ConstValForAlarm.CHANNEL_ID).apply {
            val notifyTitle = when (alarmType) {
                ConstValForAlarm.REMINDER_DATE_TIME -> context.resources.getString(R.string.notification_title_target)
                ConstValForAlarm.PRE_ALARM -> context.resources.getString(
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
        ConstValForAlarm.PRE_POST_ALARM_5M -> this.resources.getString(R.string.pre_alarm_5m)
        ConstValForAlarm.PRE_POST_ALARM_10M -> this.resources.getString(R.string.pre_alarm_10m)
        ConstValForAlarm.PRE_POST_ALARM_30M -> this.resources.getString(R.string.pre_alarm_30m)
        ConstValForAlarm.PRE_POST_ALARM_1H -> this.resources.getString(R.string.pre_alarm_1h)
        else -> this.resources.getString(R.string.pre_alarm_24h)
    }

    private fun Context.getTitleForPostAlarm(position: Int): String = when (position) {
        ConstValForAlarm.PRE_POST_ALARM_5M -> this.resources.getString(R.string.post_alarm_5m)
        ConstValForAlarm.PRE_POST_ALARM_10M -> this.resources.getString(R.string.post_alarm_10m)
        ConstValForAlarm.PRE_POST_ALARM_30M -> this.resources.getString(R.string.post_alarm_30m)
        ConstValForAlarm.PRE_POST_ALARM_1H -> this.resources.getString(R.string.post_alarm_1h)
        else -> this.resources.getString(R.string.post_alarm_24h)
    }
}

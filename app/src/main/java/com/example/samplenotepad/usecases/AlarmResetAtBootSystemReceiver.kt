package com.example.samplenotepad.usecases

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.samplenotepad.R
import com.example.samplenotepad.data.*
import com.example.samplenotepad.data.loadMemoInfoListWithReminderIO
import com.example.samplenotepad.data.resetAlarm
import com.example.samplenotepad.entities.ConstValForAlarm
import com.example.samplenotepad.entities.ConstValForMemo
import com.example.samplenotepad.entities.MemoInfo
import com.example.samplenotepad.views.display.MemoDisplayActivity
import java.text.SimpleDateFormat
import java.util.*

class AlarmResetAtBootSystemReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("場所:AlarmResetReceiver", "onReceiveが呼ばれた")

        loadMemoInfoListWithReminderIO().onEach { memoInfo ->
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val currentDate = Date()

            if (memoInfo.reminderDateTime.isNotEmpty()) {
                when (formatter.parse(memoInfo.reminderDateTime)?.compareTo(currentDate)) {
                    1 -> memoInfo.resetAlarm(context, ConstValForAlarm.REMINDER_DATE_TIME)
                    else -> memoInfo.sendNotification(context, ConstValForAlarm.REMINDER_DATE_TIME)
                }
            }

            if (memoInfo.preAlarmPosition != 0) {
                val preAlarmDate = memoInfo.getPrePostAlarmDateTime(ConstValForAlarm.PRE_ALARM)
                when (preAlarmDate.compareTo(currentDate)) {
                    1 -> memoInfo.resetAlarm(context, ConstValForAlarm.PRE_ALARM)
                    else -> memoInfo.sendNotification(context, ConstValForAlarm.PRE_ALARM)
                }
            }

            if (memoInfo.postAlarmPosition != 0) {
                val postAlarmDate = memoInfo.getPrePostAlarmDateTime(ConstValForAlarm.POST_ALARM)
                when (postAlarmDate.compareTo(currentDate)) {
                    1 -> memoInfo.resetAlarm(context, ConstValForAlarm.POST_ALARM)
                    else -> memoInfo.sendNotification(context, ConstValForAlarm.POST_ALARM)
                }
            }
        }
    }

    private fun MemoInfo.sendNotification(context: Context, alarmType: Int) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val requestCode = this.getRequestCodeForAlarm(alarmType)
        val alarmPosition = when (alarmType) {
            ConstValForAlarm.PRE_ALARM -> this.preAlarmPosition
            ConstValForAlarm.POST_ALARM -> this.postAlarmPosition
            else -> -1
        }

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
            requestCode / 10,
            requestCode,
            title,
            alarmType,
            alarmPosition
        )

        //通知が全て終わったらデータベースのMemoInfoのReminder関係の値を初期地に戻す
        when (alarmType) {
            ConstValForAlarm.REMINDER_DATE_TIME ->  {
                val intMemoInfoId = requestCode / 10
                val requestCodeForPostAlarm = intMemoInfoId * 10 + ConstValForAlarm.POST_ALARM

                //postAlarmがセットされていれば発火しない
                if (requestCodeForPostAlarm.isAlarmExist(context) == null)
                    intMemoInfoId.toLong().clearAllReminderValueInDataBaseIO()
            }
            ConstValForAlarm.POST_ALARM ->
                (requestCode/ 10).toLong().clearAllReminderValueInDataBaseIO()
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


package com.example.samplenotepad.usecases

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
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

class AlarmResetAtSystemBootReceiver : BroadcastReceiver() {

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        loadMemoInfoListWithReminderIO().onEach { memoInfo ->
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val currentDate = Date()

            if (memoInfo.reminderDateTime.isNotEmpty()) {
                when (formatter.parse(memoInfo.reminderDateTime)?.compareTo(currentDate)) {
                    1 -> memoInfo.resetAlarm(context, ConstValForAlarm.REMINDER_DATE_TIME)
                    else -> {
                        memoInfo.apply {
                            sendNotification(
                                context,
                                getRequestCodeForAlarm(ConstValForAlarm.REMINDER_DATE_TIME),
                                rowid,
                                title,
                                reminderDateTime
                            )

                            modifyMemoInfoForReminderDateTime(context)
                                .updateMemoInfoAndCancelAlarmIO(
                                    context, ConstValForAlarm.REMINDER_DATE_TIME
                                )
                        }
                    }
                }
            }

            if (memoInfo.preAlarmPosition != 0) {
                val preAlarmDate = memoInfo.getPrePostAlarmDateTime(ConstValForAlarm.PRE_ALARM)

                when (preAlarmDate.compareTo(currentDate)) {
                    1 -> memoInfo.resetAlarm(context, ConstValForAlarm.PRE_ALARM)
                    else -> memoInfo.copy(preAlarmPosition = 0)
                        .updateMemoInfoAndCancelAlarmIO(context, ConstValForAlarm.PRE_ALARM)
                }
            }

            if (memoInfo.postAlarmPosition != 0) {
                val postAlarmDate = memoInfo.getPrePostAlarmDateTime(ConstValForAlarm.POST_ALARM)

                when (postAlarmDate.compareTo(currentDate)) {
                    1 -> memoInfo.resetAlarm(context, ConstValForAlarm.POST_ALARM)
                    else -> memoInfo.copy(baseDateTimeForAlarm = "", postAlarmPosition = 0)
                        .updateMemoInfoAndCancelAlarmIO(context, ConstValForAlarm.POST_ALARM)
                }
            }
        }
    }

    private fun MemoInfo.modifyMemoInfoForReminderDateTime(context: Context): MemoInfo {
        val requestCodeForPostAlarm = this.rowid.toInt() * 10 + ConstValForAlarm.POST_ALARM

        return when (requestCodeForPostAlarm.isAlarmExist(context) == null) {
            true -> this.copy(baseDateTimeForAlarm = "", reminderDateTime = "")
            false -> this.copy(reminderDateTime = "")
        }
    }

    private fun sendNotification(
        context: Context,
        requestCode: Int,
        memoId: Long,
        memoTitle: String,
        reminderDateTime: String
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        //SDKのVersionが26以上の場合は通知チャンネルが必要
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = context.resources.getString(R.string.channel_name)
            val descriptionText = context.resources.getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val mChannel = NotificationChannel(ConstValForAlarm.CHANNEL_ID, channelName, importance)
                .apply { description = descriptionText }

            notificationManager.createNotificationChannel(mChannel)
        }

        buildNotification(context, requestCode, memoId, memoTitle, reminderDateTime)
    }


    private fun buildNotification(
        context: Context,
        notificationId: Int,
        memoId: Long,
        memoTitle: String,
        reminderDateTime: String
    ) {
        val notifyIntent = Intent(context, MemoDisplayActivity::class.java).apply {
            putExtra(ConstValForMemo.MEMO_Id, memoId)
        }
        val notifyPendingIntent = PendingIntent.getActivity(
            context, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )

        //SDKのVersionが25以下の場合はチャンネルIDは無視される
        val builder = NotificationCompat.Builder(context, ConstValForAlarm.CHANNEL_ID).apply {
            val notifyTitle = context.resources.getString(
                R.string.notification_title_miss_alarm,
                reminderDateTime.replace('-', '/')
            )

            priority = NotificationCompat.PRIORITY_MAX
            color = Color.BLUE
            setSmallIcon(R.drawable.ic_notification_small)
            setContentTitle(notifyTitle)
            setContentText(context.resources.getString(R.string.notification_text, memoTitle))
            setSubText(context.resources.getString(R.string.notification_subtitle))
            setContentIntent(notifyPendingIntent)
            setAutoCancel(true)
        }

        with(NotificationManagerCompat.from(context)) { notify(notificationId, builder.build()) }
    }
}


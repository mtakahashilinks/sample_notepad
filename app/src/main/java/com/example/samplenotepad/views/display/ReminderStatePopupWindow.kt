package com.example.samplenotepad.views.display

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.example.samplenotepad.R
import com.example.samplenotepad.entities.*
import com.example.samplenotepad.views.MemoAlertDialog
import kotlinx.android.synthetic.main.fragment_display.*
import kotlinx.android.synthetic.main.reminder_state_popup_window.view.*


private var popupWindow: PopupWindow? = null

internal fun getReminderStatesPopupWindow(displayFragment: DisplayFragment): PopupWindow =
    popupWindow
        ?: createPopupWindow(displayFragment).apply { if (popupWindow == null) popupWindow = this }

internal fun clearReminderStatesPopupWindowFlag() {
    popupWindow = null
}

internal fun PopupWindow.dismissReminderStatesPopupWindow(displayFragment: DisplayFragment) {
    this.dismiss()
    clearReminderStatesPopupWindowFlag()
    displayFragment.setIsShowingPopupWindow(false)
}

@SuppressLint("InflateParams")
private fun createPopupWindow(displayFragment: DisplayFragment): PopupWindow {
    val layoutView = displayFragment.requireActivity().layoutInflater.inflate(
        R.layout.reminder_state_popup_window, null, false
    ).apply {
        setTextForReminderPopupWindow()

        //deleteBtnの押下でリマインダーを削除する
        this.deleteBtn.setOnClickListener {
            displayFragment.showAlertDialogForDeleteReminder()
        }
    }

    return PopupWindow(displayFragment.context).apply {
        width = ViewGroup.LayoutParams.WRAP_CONTENT
        height = ViewGroup.LayoutParams.WRAP_CONTENT
        contentView = layoutView
        setBackgroundDrawable(
            ResourcesCompat.getDrawable(
                displayFragment.resources,
                R.drawable.popup_background,
                displayFragment.requireActivity().theme
            )
        )
        isOutsideTouchable = true
        isFocusable = true
        setOnDismissListener {
            clearReminderStatesPopupWindowFlag()
            displayFragment.setIsShowingPopupWindow(false)
        }
    }
}

private fun View.setTextForReminderPopupWindow() {
    val memoInfo = MemoDisplayActivity.displayViewModel.getMemoInfo()

    if (memoInfo.baseDateTimeForAlarm.isNotEmpty())
        targetDateTimeBodyTextView.setReminderDateTimeTextView(
            memoInfo.baseDateTimeForAlarm.split(" ")
        )

    preAlarmBodyTextView.setText(getPreAlarmTextFromPosition(memoInfo.preAlarmPosition))
    postAlarmBodyTextView.setText(getPostAlarmTextFromPosition(memoInfo.postAlarmPosition))
}

private fun getPreAlarmTextFromPosition(position: Int) = when (position) {
    ConstValForAlarm.PRE_POST_ALARM_NO_SET -> R.string.alarm_none
    ConstValForAlarm.PRE_POST_ALARM_5M -> R.string.pre_alarm_5m
    ConstValForAlarm.PRE_POST_ALARM_10M -> R.string.pre_alarm_10m
    ConstValForAlarm.PRE_POST_ALARM_30M -> R.string.pre_alarm_30m
    ConstValForAlarm.PRE_POST_ALARM_1H -> R.string.pre_alarm_1h
    else -> R.string.pre_alarm_24h
}

private fun getPostAlarmTextFromPosition(position: Int) = when (position) {
    ConstValForAlarm.PRE_POST_ALARM_NO_SET -> R.string.alarm_none
    ConstValForAlarm.PRE_POST_ALARM_5M -> R.string.post_alarm_5m
    ConstValForAlarm.PRE_POST_ALARM_10M -> R.string.post_alarm_10m
    ConstValForAlarm.PRE_POST_ALARM_30M -> R.string.post_alarm_30m
    ConstValForAlarm.PRE_POST_ALARM_1H -> R.string.post_alarm_1h
    else -> R.string.post_alarm_24h
}

private fun TextView.setReminderDateTimeTextView(reminderDateTimeList: List<String>) {
    val reminderDate = reminderDateTimeList[0].replace('-', '/')
    val reminderTime = reminderDateTimeList[1]
    val reminderDateTime = "$reminderDate  $reminderTime"

    this.text = reminderDateTime
}

private fun DisplayFragment.showAlertDialogForDeleteReminder() {
    MemoAlertDialog(
        R.string.dialog_delete_reminder_message,
        R.string.dialog_delete_reminder_positive_button,
        R.string.dialog_delete_reminder_negative_button,
        { dialog, id ->
            this.reminderStatesImgBtn.visibility = View.INVISIBLE

            MemoDisplayActivity.displayViewModel.apply {
                updateMemoInfo { memoInfo ->
                    memoInfo.copy(
                        baseDateTimeForAlarm = "",
                        reminderDateTime = "",
                        preAlarmPosition = 0,
                        postAlarmPosition = 0
                    )
                }.cancelAllAlarm()

                saveMemoInfo()
            }

            popupWindow?.dismissReminderStatesPopupWindow(this)
        },
        { dialog, id -> dialog.dismiss() }
    ).show(this.requireActivity().supportFragmentManager, "delete_reminder_dialog")
}

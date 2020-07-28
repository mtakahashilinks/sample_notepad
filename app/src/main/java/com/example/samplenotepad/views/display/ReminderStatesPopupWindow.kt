package com.example.samplenotepad.views.display

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import com.example.samplenotepad.R
import com.example.samplenotepad.entities.*
import com.example.samplenotepad.views.MemoAlertDialog
import kotlinx.android.synthetic.main.fragment_display_memo.*
import kotlinx.android.synthetic.main.reminder_states_popup_window.view.*


private var popupWindow: PopupWindow? = null

internal fun getReminderStatesPopupWindow(): PopupWindow =
    popupWindow ?: createPopupWindow().apply { if (popupWindow == null) popupWindow = this }

internal fun clearReminderStatesPopupWindowFlag() {
    popupWindow = null
}

internal fun PopupWindow.dismissReminderStatesPopupWindow() {
    this.dismiss()
    clearReminderStatesPopupWindowFlag()
    MemoDisplayFragment.getInstanceOrCreateNew().setIsShowingPopupWindow(false)
}

private fun createPopupWindow(): PopupWindow {
    val displayFragment = MemoDisplayFragment.getInstanceOrCreateNew()
    val layoutView = displayFragment.requireActivity().layoutInflater.inflate(
        R.layout.reminder_states_popup_window, null, false
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
            displayFragment.resources.getDrawable(
                R.drawable.popup_background, displayFragment.requireActivity().theme
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

    if (memoInfo.reminderDateTime.isNotEmpty())
        targetDateTimeBodyTextView.setTargetDateTimeTextView(memoInfo.reminderDateTime.split(" "))

    preAlarmBodyTextView.setText(getPreAlarmTextFromPosition(memoInfo.preAlarm))
    postAlarmBodyTextView.setText(getPostAlarmTextFromPosition(memoInfo.postAlarm))
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

private fun TextView.setTargetDateTimeTextView(targetDateTimeList: List<String>) {
    val targetDate = targetDateTimeList[0].replace('-', '/')
    val targetTime = targetDateTimeList[1].replace(":", " : ")
    val targetDateTime = "$targetDate  $targetTime"

    this.text = targetDateTime
}

private fun MemoDisplayFragment.showAlertDialogForDeleteReminder() {
    MemoAlertDialog(
        R.string.dialog_delete_reminder_title,
        R.string.dialog_delete_reminder_message,
        R.string.dialog_delete_reminder_positive_button,
        R.string.dialog_delete_reminder_negative_button,
        { dialog, id ->
            this.reminderStatesImgBtn.visibility = View.INVISIBLE

            MemoDisplayActivity.displayViewModel.apply {
                updateMemoInfo { memoInfo ->
                    memoInfo.copy(reminderDateTime = "", preAlarm = 0, postAlarm = 0)
                }.cancelAllAlarm()

                saveMemoInfo()
            }

            popupWindow?.dismissReminderStatesPopupWindow()
        },
        { dialog, id -> dialog.dismiss() }
    ).show(this.requireActivity().supportFragmentManager, "delete_reminder_dialog")
}

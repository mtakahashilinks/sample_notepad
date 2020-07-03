package com.example.samplenotepad.views.search

import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import com.example.samplenotepad.R
import com.example.samplenotepad.entities.*
import kotlinx.android.synthetic.main.reminder_states_popup_window.view.*


private var popupWindow: PopupWindow? = null

internal fun getReminderStatesPopupWindow(): PopupWindow =
    popupWindow ?: createPopupWindow().apply { if (popupWindow == null) popupWindow = this }

internal fun clearReminderStatesPopupWindowFlag() {
    popupWindow = null
}

internal fun PopupWindow.dismissReminderStatesPopupWindow(fragment: DisplayMemoFragment) {
    this.dismiss()
    clearReminderStatesPopupWindowFlag()
    fragment.setIsShowingPopupWindow(false)
}

private fun createPopupWindow(): PopupWindow {
    val displayFragment = DisplayMemoFragment.getInstanceOrCreateNew()
    val layoutView = displayFragment.requireActivity().layoutInflater.inflate(
        R.layout.reminder_states_popup_window, null, false
    ).apply { setTextForReminderPopupWindow() }

    layoutView.setTextForReminderPopupWindow()

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
    val memoInfo = MemoSearchActivity.searchViewModel.getMemoInfo()

    if (memoInfo.reminderDateTime.isNotEmpty())
        targetDateTimeBodyTextView.setTargetDateTimeTextView(memoInfo.reminderDateTime.split(" "))

    preAlarmBodyTextView.setText(getPreAlarmTextFromPosition(memoInfo.preAlarm))
    postAlarmBodyTextView.setText(getPostAlarmTextFromPosition(memoInfo.postAlarm))
}

private fun getPreAlarmTextFromPosition(position: Int) = when (position) {
    PRE_POST_ALARM_NO_SET -> R.string.alarm_none
    PRE_POST_ALARM_5M -> R.string.pre_alarm_5m
    PRE_POST_ALARM_10M -> R.string.pre_alarm_10m
    PRE_POST_ALARM_30M -> R.string.pre_alarm_30m
    PRE_POST_ALARM_1H -> R.string.pre_alarm_1h
    else -> R.string.pre_alarm_24h
}

private fun getPostAlarmTextFromPosition(position: Int) = when (position) {
    PRE_POST_ALARM_NO_SET -> R.string.alarm_none
    PRE_POST_ALARM_5M -> R.string.post_alarm_5m
    PRE_POST_ALARM_10M -> R.string.post_alarm_10m
    PRE_POST_ALARM_30M -> R.string.post_alarm_30m
    PRE_POST_ALARM_1H -> R.string.post_alarm_1h
    else -> R.string.post_alarm_24h
}

private fun TextView.setTargetDateTimeTextView(targetDateTimeList: List<String>) {
    val targetDate = targetDateTimeList[0].replace('-', '/')
    val targetTime = targetDateTimeList[1].replace(":", " : ")
    val targetDateTime = "$targetDate $targetTime"

    this.text = targetDateTime
}

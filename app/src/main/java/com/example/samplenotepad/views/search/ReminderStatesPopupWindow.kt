package com.example.samplenotepad.views.search

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import com.example.samplenotepad.R
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
    this.apply {
        val memoInfo = MemoSearchActivity.searchViewModel.getMemoInfo()
        val mTargetReminderTime = memoInfo.reminderTime.toString()
        //3桁の場合は頭に0を足して4桁にする
        val targetReminderTime = when (mTargetReminderTime.length == 3) {
            true -> "0".plus(mTargetReminderTime)
            false -> mTargetReminderTime
        }
        Log.d("場所:DisplayMemoFragment", "reminderDate=${memoInfo.reminderDate} reminderTime=${memoInfo.reminderTime}")

        targetDateTimeBodyTextView.setTargetDateTimeTextView(
            memoInfo.reminderDate.toString() , targetReminderTime
        )

        memoInfo.preAlarmTime.let {
            preAlarmBodyTextView.setText(getPreAlarmTextFromPosition(it))
        }

        memoInfo.postAlarmTime.let {
            postAlarmBodyTextView.setText(getPostAlarmTextFromPosition(it))
        }
    }
}

private fun getPreAlarmTextFromPosition(position: Int) = when (position) {
    0 -> R.string.alarm_none
    1 -> R.string.pre_alarm_5m
    2 -> R.string.pre_alarm_10m
    3 -> R.string.pre_alarm_30m
    4 -> R.string.pre_alarm_1h
    else -> R.string.pre_alarm_24h
}

private fun getPostAlarmTextFromPosition(position: Int) = when (position) {
    0 -> R.string.alarm_none
    1 -> R.string.post_alarm_5m
    2 -> R.string.post_alarm_10m
    3 -> R.string.post_alarm_30m
    4 -> R.string.post_alarm_1h
    else -> R.string.post_alarm_24h
}

private fun TextView.setTargetDateTimeTextView(
    targetDate: String,
    targetTime: String
) {
    this.text = String.format(
        "%s/%s/%s  %s:%s",
        targetDate.slice(0..3),
        targetDate.slice(4..5),
        targetDate.slice(6..7),
        targetTime.slice(0..1),
        targetTime.slice(2..3)
    )
}

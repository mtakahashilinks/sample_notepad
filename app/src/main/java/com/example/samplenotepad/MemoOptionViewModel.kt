package com.example.samplenotepad

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.ViewModel
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import kotlinx.android.synthetic.main.fragment_memo_option.*
import java.util.*


class MemoOptionViewModel : ViewModel() {

    companion object {
        private lateinit var optionFragment: MemoOptionFragment

        internal fun getOptionValuesForSave(): ValuesOfOptionSetting {
            fun convertDateTime(value: String): Some<Int> =
                Some(Regex("""\d+""").findAll(value).toString().toInt())

            fun getReminderParams(result: Option<Int>): Option<Int> =
                if (optionFragment.reminderOnOffSwitchView.isChecked) result else None

            return when (this::optionFragment.isInitialized) {
                true -> {
                    val title = when (optionFragment.titleTextView.text.isEmpty()) {
                        true -> "無題"
                        false -> optionFragment.titleTextView.text.toString()
                    }
                    val category = when (optionFragment.categoryTextView.text.isEmpty()) {
                        true -> "その他"
                        false -> optionFragment.categoryTextView.text.toString()
                    }
                    val targetDate =
                        getReminderParams(convertDateTime(optionFragment.reminderDateView.text.toString()))
                    val targetTime =
                        getReminderParams(convertDateTime(optionFragment.reminderTimeView.text.toString()))
                    val preAlarm =
                        getReminderParams(Some(optionFragment.preAlarmSpinnerView.selectedItemPosition))
                    val postAlarm =
                        getReminderParams(Some(optionFragment.postAlarmSpinnerView.selectedItemPosition))

                    ValuesOfOptionSetting(title, category, targetDate, targetTime, preAlarm, postAlarm)
                }
                false -> {
                    ValuesOfOptionSetting("無題", "その他", None, None, None, None)
                }
            }
        }
    }

    internal fun initOptionViewModel(fragment: MemoOptionFragment) {
        optionFragment = fragment
    }


    //Textの文字数カウンターのセット
    internal fun setCounterText(targetView: EditText, counterView: TextView, maxCharNumber: Int) {
        val formatter = "%d/${maxCharNumber}"

        counterView.text = formatter.format(0)

        targetView.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                counterView.text = formatter.format((s?.toString() ?: "0").length)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        } )
    }


    internal fun initReminderDateTime(dateView: TextView, timeView: TextView) {
        val calendar = Calendar.getInstance()

        dateView.text =
            android.text.format.DateFormat.format("yyyy/MM/dd", calendar).toString()
        timeView.text =
            android.text.format.DateFormat.format("HH : mm", calendar.time).toString()
    }


    override fun onCleared() {
        super.onCleared()
    }
}
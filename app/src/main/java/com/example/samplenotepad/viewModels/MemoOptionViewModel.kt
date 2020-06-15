package com.example.samplenotepad.viewModels

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.example.samplenotepad.R
import com.example.samplenotepad.entities.ValuesOfOptionSetting
import com.example.samplenotepad.views.main.MemoOptionFragment
import kotlinx.android.synthetic.main.fragment_memo_option.*
import java.util.*


class MemoOptionViewModel : ViewModel() {
    companion object {
        private lateinit var optionFragment: MemoOptionFragment
        private lateinit var instanceOfVM: MemoOptionViewModel

        internal fun getInstanceOrCreateNewOne(): MemoOptionViewModel =
                when (::instanceOfVM.isInitialized) {
                    true ->  instanceOfVM
                    false -> {
                        val optionViewModel =
                            ViewModelProvider.NewInstanceFactory().create(MemoOptionViewModel::class.java)
                        instanceOfVM = optionViewModel
                        optionViewModel
                    }
                }

        internal fun resetOptionStatesForCreateNewMemo() {
            fun MemoOptionFragment.resetReminderDateTime() {
                val calendar = Calendar.getInstance()

                this.reminderDateView.text =
                    android.text.format.DateFormat.format("yyyy/MM/dd", calendar).toString()
                this.reminderTimeView.text =
                    android.text.format.DateFormat.format("HH : mm", calendar.time).toString()
            }

            if (::optionFragment.isInitialized) {
                optionFragment.apply {
                    titleBodyTextView.setText("")
                    categoryTextView.setText(R.string.memo_category_default_value)
                    reminderOnOffSwitchView.isEnabled = false
                    resetReminderDateTime()
                    preAlarmSpinnerView.setSelection(0)
                    postAlarmSpinnerView.setSelection(0)
                }
            }
        }

        internal fun getOptionValuesForSave(): ValuesOfOptionSetting {
            return when (::optionFragment.isInitialized) {
                true -> {
                    val title = when (optionFragment.titleBodyTextView.text.isEmpty()) {
                        true -> None
                        false -> Some(optionFragment.titleBodyTextView.text.toString())
                    }
                    val category = when (optionFragment.categoryTextView.text.isEmpty()) {
                        true -> None
                        false -> Some(optionFragment.categoryTextView.text.toString())
                    }
                    val targetDate =
                        optionFragment.reminderDateView.text.toString().convertDateTime().getReminderParams()
                    val targetTime =
                        optionFragment.reminderTimeView.text.toString().convertDateTime().getReminderParams()
                    val preAlarm =
                        Some(optionFragment.preAlarmSpinnerView.selectedItemPosition).getReminderParams()
                    val postAlarm =
                        Some(optionFragment.postAlarmSpinnerView.selectedItemPosition).getReminderParams()

                    ValuesOfOptionSetting(title, category, targetDate, targetTime, preAlarm, postAlarm)
                }
                false -> ValuesOfOptionSetting(None, None, None, None, None, None)
            }
        }

        //例）"2020/03/05" -> 20200305
        private fun String.convertDateTime(): Some<Int> {
            val matchedResults = Regex("""\d+""").findAll(this)
            val result = matchedResults.map { it.value }.joinToString("")

            return Some(result.toInt())
        }

        private fun Option<Int>.getReminderParams(): Option<Int> =
            if (optionFragment.reminderOnOffSwitchView.isChecked) this else None
    }


    internal fun initOptionViewModel(oFragment: MemoOptionFragment) {
        optionFragment = oFragment

        oFragment.initReminderDateTime()
    }

    private fun MemoOptionFragment.initReminderDateTime() {
        val calendar = Calendar.getInstance()

        this.reminderDateView.text =
            android.text.format.DateFormat.format("yyyy/MM/dd", calendar).toString()
        this.reminderTimeView.text =
            android.text.format.DateFormat.format("HH : mm", calendar.time).toString()
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


    override fun onCleared() {
        super.onCleared()
    }
}
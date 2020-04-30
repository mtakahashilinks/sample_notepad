package com.example.samplenotepad

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.ViewModel
import arrow.core.ListK
import arrow.core.k
import java.util.*


class MemoOptionViewModel : ViewModel() {

    internal val categoryList: ListK<String> = listOf("その他").k()


    internal fun initOptionViewModel() {}


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
package com.example.samplenotepad

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import java.util.*


//Textの文字数カウンターのセット
internal fun EditText.setCounterText(counterView: TextView, maxCharNumber: Int) {
    val formatter = "%d/${maxCharNumber}"

    counterView.text = formatter.format(0)

    this.addTextChangedListener(object: TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            counterView.text = formatter.format((s?.toString() ?: "0").length)
        }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    } )
}

//現在の日付を取得
internal fun getCurrentDay(): String =
    android.text.format.DateFormat.format("yyyy/MM/dd", Calendar.getInstance()).toString()

//DatePickerDialogを表示
internal fun showDatePickerDialog(fragmentManager: FragmentManager) {
    val newFragment = DatePickerFragment()
    newFragment.show(fragmentManager, "datePicker")
}

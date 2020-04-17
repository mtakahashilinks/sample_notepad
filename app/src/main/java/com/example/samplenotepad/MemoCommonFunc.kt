package com.example.samplenotepad

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import arrow.core.None
import arrow.core.Some
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

//ボタンがクリックされた時のcheckBox処理の入り口
//internal fun MemoRow.operationCheckBox(fragment: Fragment, viewModel: MemoMainViewModel) {
//    val targetMemoRowInfo = MemoContentsOperation.getMemoContents()
//        .contentsList[MemoContentsOperation.getMemoRowIndexInList(MemoRowId(this.id))]
//    val checkBoxId = targetMemoRowInfo.checkBoxId.value
//
//    when {
//        targetMemoRowInfo.bulletId.value is Some<Int> -> {
//            MemoContentsOperation.executeMemoOperation(viewModel, DeleteBullet(fragment, this))
//            MemoContentsOperation.executeMemoOperation(viewModel, AddCheckBox(fragment, this))
//        }
//
//        checkBoxId is None ->
//            MemoContentsOperation.executeMemoOperation(viewModel, AddCheckBox(fragment, this))
//        checkBoxId is Some<Int> ->
//            MemoContentsOperation.executeMemoOperation(viewModel, DeleteCheckBox(fragment, this))
//    }
//}
//
////ボタンがクリックされた時のbullet処理の入り口
//internal fun MemoRow.operationBullet(fragment: Fragment, viewModel: MemoMainViewModel) {
//    val targetMemoRowInfo = MemoContentsOperation.getMemoContents()
//        .contentsList[MemoContentsOperation.getMemoRowIndexInList(MemoRowId(this.id))]
//    val bulletId = targetMemoRowInfo.bulletId.value
//
//    when {
//        targetMemoRowInfo.checkBoxId.value is Some<Int> -> {
//            MemoContentsOperation.executeMemoOperation(viewModel, DeleteCheckBox(fragment, this))
//            MemoContentsOperation.executeMemoOperation(viewModel, AddBullet(fragment, this))
//        }
//        bulletId is None ->
//            MemoContentsOperation.executeMemoOperation(viewModel, AddBullet(fragment, this))
//        bulletId is Some<Int> ->
//            MemoContentsOperation.executeMemoOperation(viewModel, DeleteBullet(fragment, this))
//    }
//}


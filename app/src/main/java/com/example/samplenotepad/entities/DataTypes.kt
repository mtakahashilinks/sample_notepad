package com.example.samplenotepad.entities

import android.widget.EditText
import arrow.core.*


typealias MemoRow = EditText
typealias MemoContents = ListK<MemoRowInfo>

sealed class TypeForExecuteMemoContents
data class UpdateTextOfMemoRowInfo(val memoRow: MemoRow) : TypeForExecuteMemoContents() { companion object }
data class CreateFirstMemoRow(val text: Text) : TypeForExecuteMemoContents() { companion object }
data class CreateNextMemoRow(val text: Text) : TypeForExecuteMemoContents() { companion object }
data class DeleteMemoRow(val memoRow: MemoRow) : TypeForExecuteMemoContents() { companion object }
data class AddCheckBox(val memoRow: MemoRow) : TypeForExecuteMemoContents() { companion object }
data class DeleteCheckBox(val memoRow: MemoRow) : TypeForExecuteMemoContents() { companion object }
data class ChangeCheckBoxState(val memoRow: MemoRow) : TypeForExecuteMemoContents() { companion object }
data class AddDot(val memoRow: MemoRow) : TypeForExecuteMemoContents() { companion object }
data class DeleteDot(val memoRow: MemoRow) : TypeForExecuteMemoContents() { companion object }
class ClearAll : TypeForExecuteMemoContents() { companion object }
data class SaveMemoInfo(
    val optionValues: ValuesOfOptionSetting
) : TypeForExecuteMemoContents() { companion object }


sealed class TypeForMemoRowInfo
data class MemoRowId(val value: Int) : TypeForMemoRowInfo() { companion object }
data class Text(val value: String) : TypeForMemoRowInfo() { companion object }
data class CheckBoxId(val value: Option<Int>) : TypeForMemoRowInfo() { companion object }
data class CheckBoxState(val value: Boolean) : TypeForMemoRowInfo() { companion object }
data class DotId(val value: Option<Int>) : TypeForMemoRowInfo() { companion object }

//メモの中の各行(View)の情報
data class MemoRowInfo(
    val memoRowId: MemoRowId,
    val text: Text = Text(
        ""
    ),
    val checkBoxId: CheckBoxId = CheckBoxId(
        None
    ),
    val checkBoxState: CheckBoxState = CheckBoxState(
        false
    ),
    val dotId: DotId = DotId(
        None
    )
) { companion object }


data class ValuesOfOptionSetting(
    val title: String,
    val category: String,
    val targetDate: Option<Int>,
    val targetTime: Option<Int>,
    val preAlarm: Option<Int>,
    val postAlarm: Option<Int>
) { companion object }

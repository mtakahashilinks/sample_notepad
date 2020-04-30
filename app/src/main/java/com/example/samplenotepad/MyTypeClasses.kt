package com.example.samplenotepad

import android.widget.EditText
import arrow.core.*


typealias MemoRow = EditText

sealed class TypeForExecuteMemoInfo
data class UpdateTextOfMemoRowInfo(val memoRow: MemoRow) : TypeForExecuteMemoInfo() { companion object }
data class CreateFirstMemoRow(val text: Text) : TypeForExecuteMemoInfo() { companion object }
data class CreateNextMemoRow(val text: Text) : TypeForExecuteMemoInfo() { companion object }
data class DeleteMemoRow(val memoRow: MemoRow) : TypeForExecuteMemoInfo() { companion object }
data class AddCheckBox(val memoRow: MemoRow) : TypeForExecuteMemoInfo() { companion object }
data class DeleteCheckBox(val memoRow: MemoRow) : TypeForExecuteMemoInfo() { companion object }
data class ChangeCheckBoxState(val memoRow: MemoRow) : TypeForExecuteMemoInfo() { companion object }
data class AddDot(val memoRow: MemoRow) : TypeForExecuteMemoInfo() { companion object }
data class DeleteDot(val memoRow: MemoRow) : TypeForExecuteMemoInfo() { companion object }
class ClearAll : TypeForExecuteMemoInfo() { companion object }


sealed class TypeForMemoRowInfo
data class MemoRowId(val value: Int) : TypeForMemoRowInfo() { companion object }
data class Text(val value: String) : TypeForMemoRowInfo() { companion object }
data class CheckBoxId(val value: Option<Int>) : TypeForMemoRowInfo() { companion object }
data class CheckBoxState(val value: Boolean) : TypeForMemoRowInfo() { companion object }
data class DotId(val value: Option<Int>) : TypeForMemoRowInfo() { companion object }


//メモの中の各行(View)の情報
data class MemoRowInfo(
    val memoRowId: MemoRowId,
    val text: Text = Text(""),
    val checkBoxId: CheckBoxId = CheckBoxId(None),
    val checkBoxState: CheckBoxState = CheckBoxState(false),
    val dotId: DotId = DotId(None)
) { companion object }

//各メモの情報(データベースに保存する)
data class MemoInfo(
    val id: Option<Int> = None,
    val createdDateTime: Option<Long> = None,
    val title: Option<String> = None,
    val category: Option<String> = None,
    val memoText: String ="", //データベースでメモ内容の文字列で検索する時に使う
    val contents: ListK<MemoRowInfo>,
    val reminderInfo: Option<ReminderInfo> = None
) { companion object }

//メモに登録されたリマインダーの情報
data class ReminderInfo(
    val targetDate: Long,
    val targetTime: Long,
    val preAlarmTime: Int,
    val postAlarmTime: Int
) { companion object }

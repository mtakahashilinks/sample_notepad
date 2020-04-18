package com.example.samplenotepad

import android.widget.EditText
import arrow.core.*


typealias MemoRow = EditText

sealed class ExecuteTypeForMemoContents
data class CreateFirstMemoRow(val text: Text) : ExecuteTypeForMemoContents() { companion object }
data class CreateNextMemoRow(val text: Text) : ExecuteTypeForMemoContents() { companion object }
data class DeleteMemoRow(val memoRow: MemoRow) : ExecuteTypeForMemoContents() { companion object }
data class AddCheckBox(val memoRow: MemoRow) : ExecuteTypeForMemoContents() { companion object }
data class DeleteCheckBox(val memoRow: MemoRow) : ExecuteTypeForMemoContents() { companion object }
data class AddBullet(val memoRow: MemoRow) : ExecuteTypeForMemoContents() { companion object }
data class DeleteBullet(val memoRow: MemoRow) : ExecuteTypeForMemoContents() { companion object }
class ClearAll : ExecuteTypeForMemoContents() { companion object }
class Complete : ExecuteTypeForMemoContents() { companion object }


sealed class TypeForMemoRowInfo
data class MemoRowId(val value: Int) : TypeForMemoRowInfo() { companion object }
data class Text(val value: String) : TypeForMemoRowInfo() { companion object }
data class CheckBoxId(val value: Option<Int>) : TypeForMemoRowInfo() { companion object }
data class CheckBoxState(val value: Boolean) : TypeForMemoRowInfo() { companion object }
data class BulletId(val value: Option<Int>) : TypeForMemoRowInfo() { companion object }

//メモの中の各Viewの情報
data class MemoRowInfo(
    val memoRowId: MemoRowId,
    val text: Text = Text(""),
    val checkBoxId: CheckBoxId = CheckBoxId(None),
    val checkBoxState: CheckBoxState = CheckBoxState(false),
    val bulletId: BulletId = BulletId(None)
) { companion object }

data class MemoContents(
    val contentsList: ListK<MemoRowInfo>
) { companion object }


//各メモの情報
data class MemoInfo(
    val memoId: Int,
    val createdDateTime: Long,
    val memoTitle: String,
    val memoCategory: String,
    val contents: MemoContents,
    val isRegisteredReminder: Boolean
) { companion object }

//1つの登録されたリマインダーの情報
data class ReminderInfo(
    val memoId: Int,
    val alarmTimeInAdvance: Option<Long> = None,
    val alarmTimeActual: Long,
    val isSnoozed: Boolean
) { companion object }
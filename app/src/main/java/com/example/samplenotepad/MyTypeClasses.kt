package com.example.samplenotepad

import android.widget.EditText
import arrow.core.*


typealias MemoRow = EditText

sealed class MemoRowInfoProp
class MemoRowId(val value: Int) : MemoRowInfoProp() { companion object }
class Text(val value: String) : MemoRowInfoProp() { companion object }
class CheckBoxId(val value: Option<Int>) : MemoRowInfoProp() { companion object }
class CheckBoxState(val value: Boolean) : MemoRowInfoProp() { companion object }
class BulletId(val value: Option<Int>) : MemoRowInfoProp() { companion object }


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
)

//1つの登録されたリマインダーの情報
data class ReminderInfo(
    val memoId: Int,
    val alarmTimeInAdvance: Option<Long> = None,
    val alarmTimeActual: Long,
    val isSnoozed: Boolean
)

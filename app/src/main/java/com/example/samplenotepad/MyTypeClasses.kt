package com.example.samplenotepad

import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import arrow.core.*


typealias MemoRow = EditText

sealed class ExecuteTypeForMemoContentsQueue
data class CreateFirstMemoRow(val fragment: Fragment,
                              val container: ConstraintLayout,
                              val text: Text
) : ExecuteTypeForMemoContentsQueue() { companion object }
data class CreateNextMemoRow(val fragment: Fragment,
                             val text: Text
) : ExecuteTypeForMemoContentsQueue() { companion object }
data class DeleteMemoRow(val fragment: Fragment,
                         val memoRow: MemoRow
) : ExecuteTypeForMemoContentsQueue() { companion object }
data class AddCheckBox(val fragment: Fragment,
                       val memoRow: MemoRow
) : ExecuteTypeForMemoContentsQueue() { companion object }
data class DeleteCheckBox(val fragment: Fragment,
                          val memoRow: MemoRow
) : ExecuteTypeForMemoContentsQueue() { companion object }
data class AddBullet(val fragment: Fragment,
                     val memoRow: MemoRow) : ExecuteTypeForMemoContentsQueue() { companion object }
data class DeleteBullet(val fragment: Fragment,
                        val memoRow: MemoRow) : ExecuteTypeForMemoContentsQueue() { companion object }
class ClearAll : ExecuteTypeForMemoContentsQueue() { companion object }
class Complete : ExecuteTypeForMemoContentsQueue() { companion object }


sealed class MemoRowInfoProp
data class MemoRowId(val value: Int) : MemoRowInfoProp() { companion object }
data class Text(val value: String) : MemoRowInfoProp() { companion object }
data class CheckBoxId(val value: Option<Int>) : MemoRowInfoProp() { companion object }
data class CheckBoxState(val value: Boolean) : MemoRowInfoProp() { companion object }
data class BulletId(val value: Option<Int>) : MemoRowInfoProp() { companion object }


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

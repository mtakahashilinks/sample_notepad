package com.example.samplenotepad.entities

import androidx.room.ColumnInfo
import com.example.samplenotepad.views.MemoEditText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.Serializable


//どのFragmentに依存した処理なのかを明示するため
sealed class TypeOfFragment
object EditFragment : TypeOfFragment()
object DisplayFragment : TypeOfFragment()
object NoneOfThem : TypeOfFragment()

sealed class TypeOfSearch
object BySearchWord : TypeOfSearch()
object BySearchWordAndCategory : TypeOfSearch()
object WithReminder : TypeOfSearch()
object OnCalendar : TypeOfSearch()

//MemoのViewを作成するときに新規作成なのか再編集なのか編集なし(表示のみ)なのかを判断するため
sealed class TypeOfBuildMemoOperation
object CreateNewMemo : TypeOfBuildMemoOperation()
object EditExistMemo : TypeOfBuildMemoOperation()
object DisplayExistMemo : TypeOfBuildMemoOperation()

//EditViewModelのMemoContentsの値を変更する操作で使用。どの操作なのかを判断するため。
sealed class TypeOfMemoContentsOperation
data class UpdateTextOfMemoRowInfo(
    val memoEditText: MemoEditText
) : TypeOfMemoContentsOperation() { companion object }
data class CreateFirstMemoEditText(
    val text: Text,
    val buildType: TypeOfBuildMemoOperation,
    val memoRowInfo: MemoRowInfo? = null
    ) : TypeOfMemoContentsOperation() { companion object }
data class CreateNextMemoEditText(
    val text: Text,
    val buildType: TypeOfBuildMemoOperation,
    val memoRowInfo: MemoRowInfo? = null
    ) : TypeOfMemoContentsOperation() { companion object }
data class DeleteMemoRow(
    val memoEditText: MemoEditText
) : TypeOfMemoContentsOperation() { companion object }
data class AddCheckBox(
    val memoEditText: MemoEditText,
    val buildType: TypeOfBuildMemoOperation,
    val checkBoxId: Int? = null,
    val checkBoxState: Boolean = false
) : TypeOfMemoContentsOperation() { companion object }
data class DeleteCheckBox(
    val memoEditText: MemoEditText
) : TypeOfMemoContentsOperation() { companion object }
data class ChangeCheckBoxState(
    val memoEditText: MemoEditText,
    val buildType: TypeOfBuildMemoOperation
) : TypeOfMemoContentsOperation() { companion object }
data class AddDot(
    val memoEditText: MemoEditText,
    val buildType: TypeOfBuildMemoOperation,
    val dotId: Int? = null
) : TypeOfMemoContentsOperation() { companion object }
data class DeleteDot(
    val memoEditText: MemoEditText
) : TypeOfMemoContentsOperation() { companion object }
data class SaveMemoInfo(
    val buildType: TypeOfBuildMemoOperation
) : TypeOfMemoContentsOperation()
data class SetMemoContents(
    val memoContents: MemoContents
) : TypeOfMemoContentsOperation() { companion object }
data class GetMemoContents(
    val response: CompletableDeferred<MemoContents>
) : TypeOfMemoContentsOperation() { companion object }


sealed class TypeOfMemoRowInfoValue
@Serializable
data class MemoEditTextId(val value: Int) : TypeOfMemoRowInfoValue() { companion object }
@Serializable
data class Text(val value: String) : TypeOfMemoRowInfoValue() { companion object }
@Serializable
data class CheckBoxId(val value: Int?) : TypeOfMemoRowInfoValue() { companion object }
@Serializable
data class CheckBoxState(val value: Boolean) : TypeOfMemoRowInfoValue() { companion object }
@Serializable
data class DotId(val value: Int?) : TypeOfMemoRowInfoValue() { companion object }

//メモの中の各行(View)の情報
@Serializable
data class MemoRowInfo(
    val memoEditTextId: MemoEditTextId,
    val memoText: Text = Text(""),
    val checkBoxId: CheckBoxId = CheckBoxId(null),
    val checkBoxState: CheckBoxState = CheckBoxState(false),
    val dotId: DotId = DotId(null)
) { companion object }

//メモの保存時にOption設定の値を渡すのに使用
data class StatesOfOptionSetting(
    val title: String?,
    val category: String?,
    val baseDateTimeForAlarm: String?,
    val reminderDateTime: String?,
    val preAlarmPosition: Int?,
    val postAlarmPosition: Int?
) { companion object }

//検索TOPのCategoryリストを表示するためのDataSet
data class DataSetForCategoryList(
    @ColumnInfo(name = "category") val name: String,
    @ColumnInfo(name = "COUNT(*)") val listSize: Int
) { companion object }

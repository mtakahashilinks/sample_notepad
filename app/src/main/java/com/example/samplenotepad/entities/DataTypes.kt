package com.example.samplenotepad.entities

import androidx.room.ColumnInfo
import arrow.core.*


//どのFragmentに依存した処理なのかを明示するため
sealed class WhichFragment
object EditFragment : WhichFragment()
object DisplayFragment : WhichFragment()
object NoneOfThem : WhichFragment()

//MemoのViewを作成するときに新規作成なのか再編集なのか編集なし(表示のみ)なのかを判断するため
sealed class WhichMemoExecution
object CreateNewMemo : WhichMemoExecution()
object EditExistMemo : WhichMemoExecution()
object DisplayExistMemo : WhichMemoExecution()

//EditViewModelのMemoContentsの値を変更する操作で使用。どの操作なのかを判断するため。
sealed class TypeForExecuteMemoContents
data class UpdateTextOfMemoRowInfo(
    val memoRow: MemoRow
) : TypeForExecuteMemoContents() { companion object }
data class CreateFirstMemoRow(
    val text: Text,
    val executionType: WhichMemoExecution,
    val memoRowInfo: MemoRowInfo? = null
    ) : TypeForExecuteMemoContents() { companion object }
data class CreateNextMemoRow(
    val text: Text,
    val executionType: WhichMemoExecution,
    val memoRowInfo: MemoRowInfo? = null
    ) : TypeForExecuteMemoContents() { companion object }
data class DeleteMemoRow(
    val memoRow: MemoRow
) : TypeForExecuteMemoContents() { companion object }
data class AddCheckBox(
    val memoRow: MemoRow,
    val executionType: WhichMemoExecution,
    val checkBoxId: Int? = null,
    val checkBoxState: Boolean = false
) : TypeForExecuteMemoContents() { companion object }
data class DeleteCheckBox(
    val memoRow: MemoRow
) : TypeForExecuteMemoContents() { companion object }
data class ChangeCheckBoxState(
    val memoRow: MemoRow,
    val executionType: WhichMemoExecution
) : TypeForExecuteMemoContents() { companion object }
data class AddDot(
    val memoRow: MemoRow,
    val executionType: WhichMemoExecution,
    val dotId: Int? = null
) : TypeForExecuteMemoContents() { companion object }
data class DeleteDot(
    val memoRow: MemoRow
) : TypeForExecuteMemoContents() { companion object }
data class SaveMemoInfo(
    val executionType: WhichMemoExecution
) : TypeForExecuteMemoContents()


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

//メモの保存時にOption設定の値を渡すのに使用
data class ValuesOfOptionSetting(
    val title: Option<String>,
    val category: Option<String>,
    val targetDate: Option<Int>,
    val targetTime: Option<Int>,
    val preAlarm: Option<Int>,
    val postAlarm: Option<Int>
) { companion object }

//検索TOPのCategoryリストを表示するためのDataSet
data class DataSetForCategoryList(
    @ColumnInfo(name = "category") val name: String,
    @ColumnInfo(name = "COUNT(*)") val listSize: Int
) { companion object }

//検索でメモリストを表示するためのDataSet
data class DataSetForMemoList(
    @ColumnInfo(name = "memoId") val memoInfoId: Long,
    @ColumnInfo(name = "createdDateTime") val createdDate: Long,
    @ColumnInfo(name = "title") val memoTitle: String,
    @ColumnInfo(name = "category") val memoCategory: String,
    @ColumnInfo(name = "contentsText") val memoText: String,
    @ColumnInfo(name = "reminderDate") val reminderDate: Int?
) { companion object }

package com.example.samplenotepad.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import arrow.core.extensions.list.semigroup.plus
import arrow.core.getOrElse
import arrow.core.k
import com.example.samplenotepad.R
import com.example.samplenotepad.entities.*
import com.example.samplenotepad.usecases.ReminderBroadcastReceiver
import com.example.samplenotepad.viewModels.MemoEditViewModel
import com.example.samplenotepad.viewModels.SearchViewModel
import com.example.samplenotepad.views.SampleMemoApplication
import com.example.samplenotepad.views.main.MemoOptionFragment
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import java.io.File
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*


private val showMassageForSavedFlow = MutableStateFlow<WhichFragment>(NoneOfThem)

internal fun getShowMassageForSavedFlow() = showMassageForSavedFlow

internal fun resetValueOfShowMassageForSavedFlow() {
    showMassageForSavedFlow.value = NoneOfThem
}

//Database挿入時のシリアライズ処理
//internal fun MemoContents.serializeMemoContents(): String {
//    val stBuilder = StringBuilder()
//
//    this.map { stBuilder.append(
//        ":${it.memoRowId.value},${it.text.value},${it.checkBoxId.value}," +
//                "${it.checkBoxState.value},${it.dotId.value}"
//    ) }
//
//    //完成したstBuilderから最初の「:」をdropして返す
//    return stBuilder.drop(1).toString()
//}
//
////Databaseから取得する時のデシリアライズ処理
//internal fun String.deserializeMemoContents(): MemoContents {
//    //StringをmemoRowInfo(List<String>)ごとのListにする
//    val stringListOfList: List<List<String>> = this.split(":").map { it.split(",") }
//
//    return stringListOfList.flatMap { stringList ->
//        mutableListOf<MemoRowInfo>().apply { add(MemoRowInfo(
//            MemoRowId(stringList[0].toInt()),
//            Text(stringList[1]),
//            CheckBoxId(stringList[2].toIntOrNull()),
//            CheckBoxState(stringList[3].toBoolean()),
//            DotId(stringList[4].toIntOrNull())
//        ) ) }
//    }.k()
//}

private fun MemoContents.createContentsText(): String {
    val builder = StringBuilder("")

    this.toList().onEach { memoRowInfo ->
        builder.append(memoRowInfo.text.value + '\u21B5')
    }

    return builder.toString()
}

internal fun MemoInfo.cancelAlarmOnMemoInfo() {

}

private fun RequestCode.registerAlarm(setDateTime: Calendar) {
    val application = SampleMemoApplication.instance
    val alarmManager = application.getSystemService(Context.ALARM_SERVICE) as AlarmManager?
    val intent = Intent(application.baseContext, ReminderBroadcastReceiver::class.java).apply {
        putExtra(REQUEST_CODE_FOR_ALARM, this@registerAlarm)
    }
    val pendingIntent = PendingIntent.getBroadcast(application.baseContext, this, intent, 0)

    alarmManager?.setExactAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP, setDateTime.timeInMillis, pendingIntent
    )
}

//TargetDateTime,PreAlarm,PostAlarm、それぞれ値があればアラームをセットする
private fun MemoInfo.setAlarm() {
//   if (this.reminderDateTime.isNotEmpty()) {
//       val calendarOfTargetDateTime = Calendar.getInstance().apply {
//           val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale("ja","JP","JP"))

//           formatter.parse(this@setAlarm.reminderDateTime)?.let { time = it }
//       }

//       this.getRequestCodeForAlarm(TARGET_DATE_TIME).registerAlarm(calendarOfTargetDateTime)

//       when {
//           this.preAlarm != 0 -> {
//               val preAlarmTime = calendarOfTargetDateTime.getPreAlarmTime(this.preAlarm)

//               this.getRequestCodeForAlarm(PRE_ALARM).registerAlarm(preAlarmTime)
//           }
//           this.postAlarm != 0 -> {
//               val postAlarmTime = calendarOfTargetDateTime.getPostAlarmTime(this.postAlarm)

//               this.getRequestCodeForAlarm(POST_ALARM).registerAlarm(postAlarmTime)
//           }
//       }
//   }
}

private fun MemoInfo.getRequestCodeForAlarm(reminderType: Int): Int = this.rowid.toInt() + reminderType

private fun Calendar.getPreAlarmTime(position: Int): Calendar = when (position) {
    PRE_POST_ALARM_5M -> this.apply { add(Calendar.MINUTE, -5) }
    PRE_POST_ALARM_10M -> this.apply { add(Calendar.MINUTE, -10) }
    PRE_POST_ALARM_30M -> this.apply { add(Calendar.MINUTE, -30) }
    PRE_POST_ALARM_1H -> this.apply { add(Calendar.HOUR_OF_DAY, -1) }
    else -> this.apply { add(Calendar.DATE, -1) }
}

private fun Calendar.getPostAlarmTime(position: Int) = when (position) {
    PRE_POST_ALARM_5M -> this.apply { add(Calendar.MINUTE, 5) }
    PRE_POST_ALARM_10M -> this.apply { add(Calendar.MINUTE, 10) }
    PRE_POST_ALARM_30M -> this.apply { add(Calendar.MINUTE, 30) }
    PRE_POST_ALARM_1H -> this.apply { add(Calendar.HOUR_OF_DAY, 1) }
    else -> this.apply { add(Calendar.DATE, 1) }
}

private fun MemoInfo.saveMemoInfoToDatabaseAsync(viewModel: ViewModel) = runBlocking {
    launch(Dispatchers.IO) {
        val memoInfoDao = AppDatabase.getDatabase(SampleMemoApplication.instance).memoInfoDao()

        //新規のメモはMemoInfoTableに挿入。その他はtableにアップデート。
        when (this@saveMemoInfoToDatabaseAsync.rowid) {
            0L -> {
                //databaseに新しいmemoInfoを挿入
                val rowId = memoInfoDao.insertMemoInfo(this@saveMemoInfoToDatabaseAsync)
                Log.d("場所:saveMemoInfo#挿入", "NewMemoId=${rowId} MemoContents=${Json.parse(MemoRowInfo.serializer().list, memoInfoDao.getMemoInfoById(rowId).contents)}")

                updateMemoInfoAndMemoContentsAtSavePointInViewModel(viewModel, rowId)?.setAlarm()
            }
            else -> {
                //databaseに編集したmemoInfoをアップデート
                memoInfoDao.updateMemoInfo(this@saveMemoInfoToDatabaseAsync)
                Log.d("場所:saveMemoInfo#アップデート", "MemoId=${this@saveMemoInfoToDatabaseAsync.rowid} MemoContents=${Json.parse(MemoRowInfo.serializer().list, memoInfoDao.getMemoInfoById(this@saveMemoInfoToDatabaseAsync.rowid).contents)}")

                updateMemoInfoAndMemoContentsAtSavePointInViewModel(viewModel, null)?.setAlarm()
            }
        }
    }
}

private fun MemoInfo.updateMemoInfoAndMemoContentsAtSavePointInViewModel(
    viewModel: ViewModel,
    memoId: Long?
) = when (viewModel) {
    is MemoEditViewModel -> {
        val modifiedMemoInfo = viewModel.updateMemoInfo { memoId?.let { this.copy(rowid = it) } ?: this }
        viewModel.updateMemoContentsAtSavePoint()

        modifiedMemoInfo
    }
    is SearchViewModel -> {
        val modifiedMemoInfo = viewModel.updateMemoInfo { memoId?.let { this.copy(rowid = it) } ?: this }
        viewModel.updateMemoContentsAtSavePoint()

        modifiedMemoInfo
    }
    else -> null
}

internal fun MemoInfo?.saveMemoInfo(
    executionType: WhichMemoExecution,
    viewModel: ViewModel,
    memoContents: MemoContents
) = runBlocking {
    Log.d("saveMemoInfo", "saveMemoInfoに入った")

    val stringMemoContents =
        async(Dispatchers.Default) { Json.stringify(MemoRowInfo.serializer().list, memoContents.toList()) }
    val contentsText = async(Dispatchers.Default) { memoContents.createContentsText() }
    val optionValues = MemoOptionFragment.getOptionValuesForSave()

    Log.d("saveMemoInfo", "optionValues=${optionValues}")

    val newMemoInfo = MemoInfo(
        this@saveMemoInfo?.rowid ?: 0,
        SimpleDateFormat(
            "yyyy-MM-dd HH:mm", Locale.getDefault()).format(Timestamp(System.currentTimeMillis())
        ),
        optionValues.title.getOrElse {
            SampleMemoApplication.instance.getString(R.string.memo_title_default_value)
        },
        optionValues.category.getOrElse {
            SampleMemoApplication.instance.getString(R.string.memo_category_default_value)
        },
        stringMemoContents.await(),
        contentsText.await(),
        optionValues.targetDateTime.getOrElse { "" },
        optionValues.preAlarm.getOrElse { 0 },
        optionValues.postAlarm.getOrElse { 0 }
    )

    Log.d("場所:saveMemoInfo#NewMemoInfo", "MemoId=${newMemoInfo.rowid} MemoContents=${Json.parse(MemoRowInfo.serializer().list, newMemoInfo.contents)}")

    newMemoInfo.saveMemoInfoToDatabaseAsync(viewModel).join()

    when (executionType) {
        is DisplayExistMemo -> showMassageForSavedFlow.value = DisplayFragment
        else -> showMassageForSavedFlow.value = EditFragment
    }
}

internal fun updateMemoContentsInDatabase(
    executionType: WhichMemoExecution,
    memoId: Long,
    memoContents: MemoContents
) = runBlocking {
     launch(Dispatchers.IO) {
        val memoInfoDao = AppDatabase.getDatabase(SampleMemoApplication.instance).memoInfoDao()
        val newContents = Json.stringify(MemoRowInfo.serializer().list, memoContents.toList())
        val newContentsText = memoContents.createContentsText()
        val timeStamp = System.currentTimeMillis()
        Log.d("場所:updateMemoContentsInDatabase#Update前", "MemoId=$memoId MemoContents=$memoContents")

        memoInfoDao.updateContents(memoId, timeStamp, newContents, newContentsText)

        when (executionType) {
            is CreateNewMemo -> showMassageForSavedFlow.value = EditFragment
            is DisplayExistMemo -> showMassageForSavedFlow.value = DisplayFragment
        }

        Log.d("場所:updateMemoContentsInDatabase#Update後", "MemoId=$memoId MemoContents=${Json.parse(MemoRowInfo.serializer().list, loadMemoInfoFromDatabase(memoId).contents)}")
    }
}

internal fun saveTemplateNameListToFile(templateNameList: List<String>) = runBlocking {
    val stringData = templateNameList.joinToString(separator = ",")

    launch(Dispatchers.IO) {
        SampleMemoApplication.instance.openFileOutput(
            MEMO_TEMPLATE_NAME_LIST_FILE, Context.MODE_PRIVATE
        ).use { it.write(stringData.toByteArray()) }
    }
}

internal fun loadTemplateNameListFromFile(): List<String> = runBlocking {
    val file = SampleMemoApplication.instance.getFileStreamPath(MEMO_TEMPLATE_NAME_LIST_FILE)

    withContext(Dispatchers.IO) {
        when (file.exists()) {
            true -> SampleMemoApplication.instance.openFileInput(MEMO_TEMPLATE_NAME_LIST_FILE)
                .bufferedReader().readLine().split(",")
            false -> listOf<String>()
        }
    }
}

internal fun saveTemplateToFile(
    templateName: String,
    template: MemoContents
) = runBlocking {
    val stringData = Json.stringify(MemoRowInfo.serializer().list, template.toList())

    launch(Dispatchers.IO) {
        SampleMemoApplication.instance.openFileOutput(
            MEMO_TEMPLATE_FILE + templateName, Context.MODE_PRIVATE
        ).use { it.write(stringData.toByteArray()) }
    }
}

internal fun loadTemplateFromFile(templateName: String): MemoContents = runBlocking {
    val file = SampleMemoApplication.instance.getFileStreamPath(MEMO_TEMPLATE_FILE + templateName)

    withContext(Dispatchers.IO) {
        when (file.exists()) {
            true -> Json.parse(
                MemoRowInfo.serializer().list,
                SampleMemoApplication.instance.openFileInput(MEMO_TEMPLATE_FILE + templateName)
                    .bufferedReader().readLine()).k()
            false -> throw(IllegalArgumentException("File does not exist for「$templateName」"))
        }
    }
}

internal fun deleteTemplateFile(templateName: String) {
    val file = SampleMemoApplication.instance.getFileStreamPath(MEMO_TEMPLATE_FILE + templateName)

    file.delete()
}

internal fun renameTemplateFile(oldTemplateName: String, newTemplateName: String) {
    val application = SampleMemoApplication.instance
    val from = File(application.filesDir, MEMO_TEMPLATE_FILE + oldTemplateName)
    val to = File(application.filesDir, MEMO_TEMPLATE_FILE + newTemplateName)

    from.renameTo(to)
}

internal fun searchMemoInfoForSearchTopInDatabase(
    word: String
): List<DataSetForMemoList> = runBlocking {
    withContext(Dispatchers.IO) {
        val memoInfoDao = AppDatabase.getDatabase(SampleMemoApplication.instance).memoInfoDao()
        val searchWord = "%$word%"

        memoInfoDao.searchMemoInfoForSearchTop(searchWord)
    }
}

internal fun searchMemoInfoForSearchInACategoryInDatabase(
    category: String,
    word: String
): List<DataSetForMemoList> = runBlocking {
    withContext(Dispatchers.IO) {
        val memoInfoDao = AppDatabase.getDatabase(SampleMemoApplication.instance).memoInfoDao()
        val searchWord = "%$word%"

        memoInfoDao.searchMemoInfoForSearchInACategory(category, searchWord)
    }
}

internal fun loadMemoInfoFromDatabase(memoInfoId: Long): MemoInfo = runBlocking {
    withContext(Dispatchers.IO) {
        val memoInfoDao = AppDatabase.getDatabase(SampleMemoApplication.instance).memoInfoDao()

        memoInfoDao.getMemoInfoById(memoInfoId)
    }
}

internal fun loadCategoryListFromDatabase(): List<String> = runBlocking {
    val categoryList = withContext(Dispatchers.IO) {
        AppDatabase.getDatabase(SampleMemoApplication.instance).memoInfoDao().getCategoryList()
    }
    val defaultCategoryName = SampleMemoApplication.instance.getString(R.string.memo_category_default_value)

    //defaultCategoryがListに含まれているかの条件分けと、defaultCategoryをListの先頭にする処理
    when (categoryList.contains(defaultCategoryName)) {
        true -> {
            val list = listOf(defaultCategoryName).plus(categoryList.filterNot { it == defaultCategoryName })
            Log.d("場所:loadCategoryList", "categoryList=$list")
            list
        }
        false -> {
            val list = listOf(defaultCategoryName).plus(categoryList)
            Log.d("場所:loadCategoryList", "categoryList=$list")
            list
        }
    }
}

//categoryとその中に含まれるメモの数のタプルのリスト
internal fun loadDataSetForCategoryListFromDatabase(): List<DataSetForCategoryList> = runBlocking {
    withContext(Dispatchers.IO) {
        val memoInfoDao = AppDatabase.getDatabase(SampleMemoApplication.instance).memoInfoDao()
        val defaultCategoryName =
            SampleMemoApplication.instance.getString(R.string.memo_category_default_value)
        val list = memoInfoDao.getDataSetForCategoryList()
        Log.d("場所:loadDataSetForCategoryListFromDatabase", "list=$list")

        //defaultCategoryがListに含まれているかの条件分けと、defaultCategoryをListの先頭にする処理
        when (list.firstOrNull { it.name == defaultCategoryName } == null) {
            true -> {
                val sortedList = listOf(DataSetForCategoryList(defaultCategoryName, 0)).plus(list)
                Log.d("場所:loadDataSetForCategoryListFromDatabase#true", "sortedList=$sortedList")
                sortedList
            }
            false -> {
                val targetIndex = list.indexOfFirst { it.name == defaultCategoryName }
                val targetValue = list[targetIndex]
                val sortedList = listOf(targetValue).plus(list.filterNot { it.name == defaultCategoryName })

                Log.d("場所:loadDataSetForCategoryListFromDatabase#false", "sortedList=$sortedList")
                sortedList
            }
        }
    }
}

internal fun loadDataSetForMemoListFromDatabase(
    category: String
): List<DataSetForMemoList> = runBlocking {
    withContext(Dispatchers.IO) {
        val memoInfoDao = AppDatabase.getDatabase(SampleMemoApplication.instance).memoInfoDao()

        memoInfoDao.getDataSetForMemoList(category)
    }
}

internal fun renameCategory(oldCategoryName: String, newCategoryName: String) = runBlocking {
    val memoInfoDao = AppDatabase.getDatabase(SampleMemoApplication.instance).memoInfoDao()

    launch(Dispatchers.IO) { memoInfoDao.renameCategory(oldCategoryName, newCategoryName) }
}


internal fun deleteMemoByCategoryFromDatabase(category: String) = runBlocking {
    val memoInfoDao = AppDatabase.getDatabase(SampleMemoApplication.instance).memoInfoDao()

    launch(Dispatchers.IO) { memoInfoDao.deleteByCategory(category) }
}

internal fun deleteMemoByIdFromDatabase(id: Long) = runBlocking {
    val memoInfoDao = AppDatabase.getDatabase(SampleMemoApplication.instance).memoInfoDao()

    launch(Dispatchers.IO) { memoInfoDao.deleteById(id) }
}

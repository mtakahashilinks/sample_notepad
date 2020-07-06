package com.example.samplenotepad.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import arrow.core.extensions.list.semigroup.plus
import arrow.core.k
import com.example.samplenotepad.R
import com.example.samplenotepad.entities.*
import com.example.samplenotepad.usecases.ReminderBroadcastReceiver
import com.example.samplenotepad.viewModels.MemoEditViewModel
import com.example.samplenotepad.viewModels.SearchViewModel
import com.example.samplenotepad.views.SampleMemoApplication
import com.example.samplenotepad.views.main.MemoOptionFragment
import kotlinx.coroutines.*
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import java.io.File
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*


private fun MemoContents.createContentsText(): String {
    val builder = StringBuilder("")

    this.toList().onEach { memoRowInfo ->
        builder.append(memoRowInfo.memoText.value + '\u21B5')
    }

    return builder.toString()
}

internal fun MemoInfo.cancelAlarmOnMemoInfoIO() {

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

                updateMemoInfoInViewModel(viewModel, rowId)?.setAlarm()
            }
            else -> {
                //databaseに編集したmemoInfoをアップデート
                memoInfoDao.updateMemoInfo(this@saveMemoInfoToDatabaseAsync)
                Log.d("場所:saveMemoInfo#アップデート", "MemoId=${this@saveMemoInfoToDatabaseAsync.rowid} MemoContents=${Json.parse(MemoRowInfo.serializer().list, memoInfoDao.getMemoInfoById(this@saveMemoInfoToDatabaseAsync.rowid).contents)}")

                updateMemoInfoInViewModel(viewModel, null)?.setAlarm()
            }
        }
    }
}

private fun MemoInfo.updateMemoInfoInViewModel(
    viewModel: ViewModel,
    memoId: Long?
) = when (viewModel) {
        is MemoEditViewModel -> viewModel.updateMemoInfo { memoId?.let { this.copy(rowid = it) } ?: this }
        is SearchViewModel -> viewModel.updateMemoInfo { this }
        else -> null
    }

private fun MemoInfo?.createNewMemoInfo(
    title: String?,
    category: String?,
    stringMemoContents: String,
    contentsTextForSearch: String,
    reminderDateTime: String?,
    preAlarmPosition: Int?,
    postAlarmPosition: Int?
) = MemoInfo(
        this?.rowid ?: 0,
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            .format(Timestamp(System.currentTimeMillis())),
        title ?: SampleMemoApplication.instance.getString(R.string.memo_title_default_value),
        category ?: SampleMemoApplication.instance.getString(R.string.memo_category_default_value),
        stringMemoContents,
        contentsTextForSearch,
        reminderDateTime ?: "",
        preAlarmPosition ?: 0,
        postAlarmPosition ?: 0
    )

internal fun MemoInfo?.saveMemoInfoIO(viewModel: ViewModel, memoContents: MemoContents) = runBlocking {
    Log.d("saveMemoInfo", "saveMemoInfoに入った")

    val stringMemoContents =
        async(Dispatchers.Default) { Json.stringify(MemoRowInfo.serializer().list, memoContents) }
    val contentsTextForSearch = async(Dispatchers.Default) { memoContents.createContentsText() }

    val newMemoInfo = when (viewModel) {
        is MemoEditViewModel -> {
            val optionValues = MemoOptionFragment.getOptionValuesForSave()

            this@saveMemoInfoIO.createNewMemoInfo(
                optionValues?.title,
                optionValues?.category,
                stringMemoContents.await(),
                contentsTextForSearch.await(),
                optionValues?.targetDateTime,
                optionValues?.preAlarm,
                optionValues?.postAlarm
            )
        }
        else -> {
            this@saveMemoInfoIO.createNewMemoInfo(
                this@saveMemoInfoIO?.title,
                this@saveMemoInfoIO?.category,
                stringMemoContents.await(),
                contentsTextForSearch.await(),
                this@saveMemoInfoIO?.reminderDateTime,
                this@saveMemoInfoIO?.preAlarm,
                this@saveMemoInfoIO?.postAlarm
            )
        }
    }

    Log.d("saveMemoInfo", "NewMemoInfo=$newMemoInfo")

    newMemoInfo.saveMemoInfoToDatabaseAsync(viewModel).join()
}


internal fun saveTemplateNameListToFileIO(templateNameList: List<String>) = runBlocking {
    val stringData = templateNameList.joinToString(separator = ",")

    launch(Dispatchers.IO) {
        SampleMemoApplication.instance.openFileOutput(
            MEMO_TEMPLATE_NAME_LIST_FILE, Context.MODE_PRIVATE
        ).use { it.write(stringData.toByteArray()) }
    }
}

internal fun loadTemplateNameListFromFileIO(): List<String> = runBlocking {
    val file = SampleMemoApplication.instance.getFileStreamPath(MEMO_TEMPLATE_NAME_LIST_FILE)

    withContext(Dispatchers.IO) {
        when (file.exists()) {
            true -> SampleMemoApplication.instance.openFileInput(MEMO_TEMPLATE_NAME_LIST_FILE)
                .bufferedReader().readLine().split(",")
            false -> listOf<String>()
        }
    }
}

internal fun saveTemplateToFileIO(
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

internal fun loadTemplateFromFileIO(templateName: String): MemoContents = runBlocking {
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

internal fun deleteTemplateFileIO(templateName: String) {
    val file = SampleMemoApplication.instance.getFileStreamPath(MEMO_TEMPLATE_FILE + templateName)

    file.delete()
}

internal fun renameTemplateFileIO(oldTemplateName: String, newTemplateName: String) {
    val application = SampleMemoApplication.instance
    val from = File(application.filesDir, MEMO_TEMPLATE_FILE + oldTemplateName)
    val to = File(application.filesDir, MEMO_TEMPLATE_FILE + newTemplateName)

    from.renameTo(to)
}

internal fun searchingMemoInfoWithAWordIO(
    word: String
): List<DataSetForMemoList> = runBlocking {
    withContext(Dispatchers.IO) {
        val memoInfoDao = AppDatabase.getDatabase(SampleMemoApplication.instance).memoInfoDao()
        val searchWord = "%$word%"

        memoInfoDao.searchMemoInfoForSearchTop(searchWord)
    }
}

internal fun searchingMemoInfoWithAWordAndCategoryIO(
    category: String,
    word: String
): List<DataSetForMemoList> = runBlocking {
    withContext(Dispatchers.IO) {
        val memoInfoDao = AppDatabase.getDatabase(SampleMemoApplication.instance).memoInfoDao()
        val searchWord = "%$word%"

        memoInfoDao.searchMemoInfoForSearchInACategory(category, searchWord)
    }
}

internal fun loadMemoInfoIO(memoInfoId: Long): MemoInfo = runBlocking {
    withContext(Dispatchers.IO) {
        val memoInfoDao = AppDatabase.getDatabase(SampleMemoApplication.instance).memoInfoDao()

        memoInfoDao.getMemoInfoById(memoInfoId)
    }
}

internal fun loadCategoryListIO(): List<String> = runBlocking {
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
internal fun loadDataSetForCategoryListIO(): List<DataSetForCategoryList> = runBlocking {
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

internal fun loadDataSetForMemoListIO(
    category: String
): List<DataSetForMemoList> = runBlocking {
    withContext(Dispatchers.IO) {
        val memoInfoDao = AppDatabase.getDatabase(SampleMemoApplication.instance).memoInfoDao()

        memoInfoDao.getDataSetForMemoList(category)
    }
}

internal fun renameCategoryIO(oldCategoryName: String, newCategoryName: String) = runBlocking {
    val memoInfoDao = AppDatabase.getDatabase(SampleMemoApplication.instance).memoInfoDao()

    launch(Dispatchers.IO) { memoInfoDao.renameCategory(oldCategoryName, newCategoryName) }
}


internal fun deleteMemoByCategoryIO(category: String) = runBlocking {
    val memoInfoDao = AppDatabase.getDatabase(SampleMemoApplication.instance).memoInfoDao()

    launch(Dispatchers.IO) { memoInfoDao.deleteByCategory(category) }
}

internal fun deleteMemoByIdIO(id: Long) = runBlocking {
    val memoInfoDao = AppDatabase.getDatabase(SampleMemoApplication.instance).memoInfoDao()

    launch(Dispatchers.IO) { memoInfoDao.deleteById(id) }
}

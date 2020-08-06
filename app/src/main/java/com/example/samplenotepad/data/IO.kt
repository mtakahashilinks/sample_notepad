package com.example.samplenotepad.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.samplenotepad.R
import com.example.samplenotepad.entities.*
import com.example.samplenotepad.usecases.ReminderNotificationReceiver
import com.example.samplenotepad.viewModels.MemoDisplayViewModel
import com.example.samplenotepad.viewModels.MemoEditViewModel
import com.example.samplenotepad.views.SampleMemoApplication
import com.example.samplenotepad.views.main.MemoOptionFragment
import kotlinx.coroutines.*
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import java.io.File
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*


private fun MemoInfo.cancelAlarm(
    context: Context,
    alarmType: Int
) = runBlocking {
    val memoInfoDao = AppDatabase.getDatabase(SampleMemoApplication.instance).memoInfoDao()
    val requestCode = this@cancelAlarm.getRequestCodeForAlarm(alarmType)
    val pendingIntent = requestCode.isAlarmExist(context)

    //アラームがセットされてればキャンセル
    if (pendingIntent != null) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarmManager.cancel(pendingIntent)
    }

    //DatabaseのReminder関係の値をUpdate
    when (alarmType) {
        ConstValForAlarm.REMINDER_DATE_TIME -> {
            when (this@cancelAlarm.postAlarmPosition == 0) {
                true -> memoInfoDao.updateMemoInfoDao(
                    this@cancelAlarm.copy(baseDateTimeForAlarm = "", reminderDateTime = "")
                )
                false -> memoInfoDao.updateMemoInfoDao(this@cancelAlarm.copy(reminderDateTime = ""))
            }
        }
        ConstValForAlarm.PRE_ALARM ->
            memoInfoDao.updateMemoInfoDao(this@cancelAlarm.copy(preAlarmPosition = 0))
        ConstValForAlarm.POST_ALARM -> {
            when (this@cancelAlarm.reminderDateTime == "") {
                true -> memoInfoDao.updateMemoInfoDao(
                    this@cancelAlarm.copy(baseDateTimeForAlarm = "", postAlarmPosition = 0)
                )
                false -> memoInfoDao.updateMemoInfoDao(this@cancelAlarm.copy(postAlarmPosition = 0))
            }
        }
    }
}

internal fun MemoInfo.cancelAlarmIO(context: Context, alarmType: Int) {
    this.cancelAlarm(context, alarmType)
}

internal fun MemoInfo.cancelAllAlarmIO(context: Context) {
    if (this.reminderDateTime.isNotEmpty())
        this.cancelAlarm(context, ConstValForAlarm.REMINDER_DATE_TIME)

    if (this.preAlarmPosition != 0)
        this.cancelAlarm(context, ConstValForAlarm.PRE_ALARM)

    if (this.postAlarmPosition != 0)
        this.cancelAlarm(context, ConstValForAlarm.POST_ALARM)
}

//returnがnull以外ならAlarmはセットされている
internal fun RequestCode.isAlarmExist(context: Context): PendingIntent? {
    val intent = Intent(context, ReminderNotificationReceiver::class.java)

    return PendingIntent.getBroadcast(context, this, intent, PendingIntent.FLAG_NO_CREATE)
}

//DBのMemoInfoのReminder関係の値を初期値に戻す
internal fun MemoInfoId.clearAllReminderValueInDataBaseIO() = runBlocking {
    withContext(Dispatchers.IO) {
        val memoInfoDao = AppDatabase.getDatabase(SampleMemoApplication.instance).memoInfoDao()

        memoInfoDao.clearAllReminderValueByIdDao(this@clearAllReminderValueInDataBaseIO)
    }
}

internal fun MemoInfo.resetAlarm(context:Context, reminderType: Int) {
    val requestCode = this.getRequestCodeForAlarm(reminderType)

    when (reminderType) {
        ConstValForAlarm.REMINDER_DATE_TIME -> {
            getReminderDateTimeCalendar(this.reminderDateTime)
                .registerAlarm(context, requestCode, this.title)
        }
        ConstValForAlarm.PRE_ALARM -> {
            getReminderDateTimeCalendar(this.baseDateTimeForAlarm)
                .getPreAlarmCalendar(this.preAlarmPosition)
                .registerAlarm(context, requestCode, this.title, this.preAlarmPosition)
        }
        else -> {
            getReminderDateTimeCalendar(this.baseDateTimeForAlarm)
                .getPostAlarmCalendar(this.postAlarmPosition)
                .registerAlarm(context, requestCode, this.title, this.postAlarmPosition)
        }
    }
}

//Alarmを登録する
private fun Calendar.registerAlarm(
    context: Context,
    requestCode: Int,
    memoTitle: String,
    alarmPosition: Int = -1
) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, ReminderNotificationReceiver::class.java).apply {
        putExtra(ConstValForAlarm.REQUEST_CODE, requestCode)
        putExtra(ConstValForAlarm.MEMO_TITLE, memoTitle)
        putExtra(ConstValForAlarm.ALARM_POSITION, alarmPosition)
    }
    val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, 0)

    alarmManager.setExactAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP, this.timeInMillis, pendingIntent
    )
}


//ReminderDateTime,PreAlarm,PostAlarm、それぞれ値があればアラームをセットする
private fun MemoInfo.setAlarm(context: Context) {
    //reminderDateTimeがあればアラームをセット
    when (this.reminderDateTime.isNotEmpty()) {
       true ->
           getReminderDateTimeCalendar(this.reminderDateTime).registerAlarm(
               context,
               this.getRequestCodeForAlarm(ConstValForAlarm.REMINDER_DATE_TIME),
               this.title
           )
       false -> this.cancelAlarm(context, ConstValForAlarm.REMINDER_DATE_TIME)
   }

    //preAlarmがあればアラームをセット
    when (this.preAlarmPosition != 0) {
        true -> {
            val preAlarmTimeCalendar = getReminderDateTimeCalendar(this.baseDateTimeForAlarm)
                .getPreAlarmCalendar(this.preAlarmPosition)

            preAlarmTimeCalendar.registerAlarm(
                context,
                this.getRequestCodeForAlarm(ConstValForAlarm.PRE_ALARM),
                this.title,
                this.preAlarmPosition
            )
        }
        false -> this.cancelAlarm(context, ConstValForAlarm.PRE_ALARM)
    }

    //postAlarmがあればアラームをセット
    when (this.postAlarmPosition != 0) {
        true -> {
            val postAlarmTimeCalendar = getReminderDateTimeCalendar(this.baseDateTimeForAlarm)
                .getPostAlarmCalendar(this.postAlarmPosition)

            postAlarmTimeCalendar.registerAlarm(
                context,
                this.getRequestCodeForAlarm(ConstValForAlarm.POST_ALARM),
                this.title,
                this.postAlarmPosition
            )
        }
        false -> this.cancelAlarm(context, ConstValForAlarm.POST_ALARM)
    }
}

private fun getReminderDateTimeCalendar(dateTime: String): Calendar =
    Calendar.getInstance().apply {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        formatter.parse(dateTime)?.let { time = it }
    }

internal fun MemoInfo.getPrePostAlarmDateTime(alarmType: Int): Date =
    when (alarmType) {
        ConstValForAlarm.PRE_ALARM ->
            getReminderDateTimeCalendar(this.baseDateTimeForAlarm)
                .getPreAlarmCalendar(this.preAlarmPosition).time
        ConstValForAlarm.POST_ALARM ->
            getReminderDateTimeCalendar(this.baseDateTimeForAlarm)
                .getPostAlarmCalendar(this.postAlarmPosition).time
        else -> Date()
    }

internal fun MemoInfo.getRequestCodeForAlarm(alarmType: Int): Int =
    this.rowid.toInt() * 10 + alarmType

private fun Calendar.getPreAlarmCalendar(position: Int): Calendar = when (position) {
    ConstValForAlarm.PRE_POST_ALARM_5M -> this.apply { add(Calendar.MINUTE, -1);TODO("-5に戻す") }
    ConstValForAlarm.PRE_POST_ALARM_10M -> this.apply { add(Calendar.MINUTE, -10) }
    ConstValForAlarm.PRE_POST_ALARM_30M -> this.apply { add(Calendar.MINUTE, -30) }
    ConstValForAlarm.PRE_POST_ALARM_1H -> this.apply { add(Calendar.HOUR_OF_DAY, -1) }
    else -> this.apply { add(Calendar.DATE, -1) }
}

private fun Calendar.getPostAlarmCalendar(position: Int) = when (position) {
    ConstValForAlarm.PRE_POST_ALARM_5M -> this.apply { add(Calendar.MINUTE, 1);TODO("5に戻す") }
    ConstValForAlarm.PRE_POST_ALARM_10M -> this.apply { add(Calendar.MINUTE, 10) }
    ConstValForAlarm.PRE_POST_ALARM_30M -> this.apply { add(Calendar.MINUTE, 30) }
    ConstValForAlarm.PRE_POST_ALARM_1H -> this.apply { add(Calendar.HOUR_OF_DAY, 1) }
    else -> this.apply { add(Calendar.DATE, 1) }
}

private fun MemoInfo.saveMemoInfoToDatabaseAsync(viewModel: ViewModel) = runBlocking {
    launch(Dispatchers.IO) {
        val memoInfoDao = AppDatabase.getDatabase(SampleMemoApplication.instance).memoInfoDao()
        val context = SampleMemoApplication.instance.baseContext

        //新規のメモはMemoInfoTableに挿入。その他はtableにアップデート。
        when (this@saveMemoInfoToDatabaseAsync.rowid) {
            0L -> {
                //databaseに新しいmemoInfoを挿入
                val rowId = memoInfoDao.insertMemoInfoDao(this@saveMemoInfoToDatabaseAsync)
                Log.d("場所:saveMemoInfo#挿入", "NewMemoId=${rowId} MemoContents=${Json.parse(MemoRowInfo.serializer().list, memoInfoDao.getMemoInfoByIdDao(rowId).contents)}")

                this@saveMemoInfoToDatabaseAsync
                    .updateMemoInfoInViewModel(viewModel, rowId)?.setAlarm(context)
            }
            else -> {
                //databaseに編集したmemoInfoをアップデート
                memoInfoDao.updateMemoInfoDao(this@saveMemoInfoToDatabaseAsync)
                Log.d("場所:saveMemoInfo#アップデート", "MemoId=${this@saveMemoInfoToDatabaseAsync.rowid} MemoContents=${Json.parse(MemoRowInfo.serializer().list, memoInfoDao.getMemoInfoByIdDao(this@saveMemoInfoToDatabaseAsync.rowid).contents)}")

                this@saveMemoInfoToDatabaseAsync
                    .updateMemoInfoInViewModel(viewModel, null)?.setAlarm(context)
            }
        }
    }
}

private fun MemoInfo.updateMemoInfoInViewModel(
    viewModel: ViewModel,
    memoId: Long?
) = when (viewModel) {
        is MemoEditViewModel -> viewModel.updateMemoInfo { memoId?.let { this.copy(rowid = it) } ?: this }
        is MemoDisplayViewModel -> viewModel.updateMemoInfo { this }
        else -> null
    }

private fun MemoInfo?.createNewMemoInfo(
    title: String?,
    category: String?,
    stringMemoContents: String,
    contentsTextForSearch: String,
    baseDateTimeForAlarm: String?,
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
    baseDateTimeForAlarm ?: "",
    reminderDateTime ?: "",
    preAlarmPosition ?: 0,
    postAlarmPosition ?: 0
)

private fun MemoContents.createContentsText(): String {
    val builder = StringBuilder("")

    this.toList().onEach { memoRowInfo ->
        builder.append(memoRowInfo.memoText.value + '\u21B5')
    }

    return builder.toString()
}

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
                optionValues?.baseDateTimeForAlarm,
                optionValues?.reminderDateTime,
                optionValues?.preAlarmPosition,
                optionValues?.postAlarmPosition
            )
        }
        else -> {
            this@saveMemoInfoIO.createNewMemoInfo(
                this@saveMemoInfoIO?.title,
                this@saveMemoInfoIO?.category,
                stringMemoContents.await(),
                contentsTextForSearch.await(),
                this@saveMemoInfoIO?.baseDateTimeForAlarm,
                this@saveMemoInfoIO?.reminderDateTime,
                this@saveMemoInfoIO?.preAlarmPosition,
                this@saveMemoInfoIO?.postAlarmPosition
            )
        }
    }

    Log.d("saveMemoInfo", "NewMemoInfo=$newMemoInfo")

    newMemoInfo.saveMemoInfoToDatabaseAsync(viewModel).join()
}


internal fun saveTemplateNameListIO(templateNameList: List<String>) = runBlocking {
    val stringData = templateNameList.joinToString(separator = ",")

    launch(Dispatchers.IO) {
        SampleMemoApplication.instance.openFileOutput(
            ConstValForMemo.TEMPLATE_NAME_LIST_FILE, Context.MODE_PRIVATE
        ).use { it.write(stringData.toByteArray()) }
    }
}

internal fun loadTemplateNameListIO(): List<String> = runBlocking {
    val file = SampleMemoApplication.instance.getFileStreamPath(ConstValForMemo.TEMPLATE_NAME_LIST_FILE)

    withContext(Dispatchers.IO) {
        when (file.exists()) {
            true -> SampleMemoApplication.instance.openFileInput(ConstValForMemo.TEMPLATE_NAME_LIST_FILE)
                .bufferedReader().readLine().split(",")
            false -> listOf<String>()
        }
    }
}

internal fun saveTemplateBodyIO(
    templateName: String,
    template: MemoContents
) = runBlocking {
    val stringData = Json.stringify(MemoRowInfo.serializer().list, template.toList())

    launch(Dispatchers.IO) {
        SampleMemoApplication.instance.openFileOutput(
            ConstValForMemo.TEMPLATE_FILE + templateName, Context.MODE_PRIVATE
        ).use { it.write(stringData.toByteArray()) }
    }
}

internal fun loadTemplateBodyIO(templateName: String): MemoContents = runBlocking {
    val file = SampleMemoApplication.instance.getFileStreamPath(ConstValForMemo.TEMPLATE_FILE + templateName)

    withContext(Dispatchers.IO) {
        when (file.exists()) {
            true -> Json.parse(
                MemoRowInfo.serializer().list,
                SampleMemoApplication.instance.openFileInput(ConstValForMemo.TEMPLATE_FILE + templateName)
                    .bufferedReader().readLine())
            false -> throw(IllegalArgumentException("File does not exist for「$templateName」"))
        }
    }
}

internal fun deleteTemplateIO(templateName: String) {
    val file = SampleMemoApplication.instance.getFileStreamPath(ConstValForMemo.TEMPLATE_FILE + templateName)

    file.delete()
}

internal fun renameTemplateIO(oldTemplateName: String, newTemplateName: String) {
    val application = SampleMemoApplication.instance
    val from = File(application.filesDir, ConstValForMemo.TEMPLATE_FILE + oldTemplateName)
    val to = File(application.filesDir, ConstValForMemo.TEMPLATE_FILE + newTemplateName)

    from.renameTo(to)
}

internal fun loadMemoInfoListBySearchWordIO(
    searchWord: String
): List<MemoInfo> = runBlocking {
    withContext(Dispatchers.IO) {
        val memoInfoDao = AppDatabase.getDatabase(SampleMemoApplication.instance).memoInfoDao()
        val searchWord = "%$searchWord%"

        memoInfoDao.getMemoInfoListBySearchWordDao(searchWord)
    }
}

internal fun loadMemoInfoListBySearchWordAndCategoryIO(
    category: String,
    searchWord: String
): List<MemoInfo> = runBlocking {
    withContext(Dispatchers.IO) {
        val memoInfoDao = AppDatabase.getDatabase(SampleMemoApplication.instance).memoInfoDao()
        val searchWord = "%$searchWord%"

        memoInfoDao.getMemoInfoListBySearchWordAndCategoryDao(category, searchWord)
    }
}

internal fun loadMemoInfoListWithReminderIO(): List<MemoInfo> = runBlocking {
    withContext(Dispatchers.IO) {
        val memoInfoDao = AppDatabase.getDatabase(SampleMemoApplication.instance).memoInfoDao()

        memoInfoDao.getMemoInfoListWithReminderDao()
    }
}

internal fun loadMemoInfoListBySearchWordWithReminderIO(
    searchWord: String
): List<MemoInfo> = runBlocking {
    withContext(Dispatchers.IO) {
        val memoInfoDao = AppDatabase.getDatabase(SampleMemoApplication.instance).memoInfoDao()
        val searchWord = "%$searchWord%"

        memoInfoDao.getMemoInfoListBySearchWordWithReminderDao(searchWord)
    }
}

internal fun loadMemoInfoIO(memoInfoId: Long): MemoInfo = runBlocking {
    withContext(Dispatchers.IO) {
        val memoInfoDao = AppDatabase.getDatabase(SampleMemoApplication.instance).memoInfoDao()

        memoInfoDao.getMemoInfoByIdDao(memoInfoId)
    }
}

internal fun loadCategoryListIO(): List<String> = runBlocking {
    val categoryList = withContext(Dispatchers.IO) {
        AppDatabase.getDatabase(SampleMemoApplication.instance).memoInfoDao().getCategoryListDao()
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
        val list = memoInfoDao.getDataSetForCategoryListDao()
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

internal fun loadDataSetForMemoListByCategoryIO(
    category: String
): List<MemoInfo> = runBlocking {
    withContext(Dispatchers.IO) {
        val memoInfoDao = AppDatabase.getDatabase(SampleMemoApplication.instance).memoInfoDao()

        memoInfoDao.getMemoInfoListByCategoryDao(category)
    }
}

internal fun renameCategoryIO(oldCategoryName: String, newCategoryName: String) = runBlocking {
    val memoInfoDao = AppDatabase.getDatabase(SampleMemoApplication.instance).memoInfoDao()

    launch(Dispatchers.IO) { memoInfoDao.updateCategoryDao(oldCategoryName, newCategoryName) }
}


internal fun deleteMemoByCategoryIO(category: String) = runBlocking {
    val memoInfoDao = AppDatabase.getDatabase(SampleMemoApplication.instance).memoInfoDao()

    launch(Dispatchers.IO) {
        memoInfoDao.apply {
            //削除する前にMemoInfoにAlarmがセットされていたらcancelする
            getMemoInfoListByCategoryDao(category).onEach { memoInfo ->
                memoInfo.cancelAllAlarmIO(SampleMemoApplication.instance.baseContext)
            }

            deleteMemoInfoByCategoryDao(category)
        }
    }
}

internal fun deleteMemoByIdIO(id: Long) = runBlocking {
    val memoInfoDao = AppDatabase.getDatabase(SampleMemoApplication.instance).memoInfoDao()

    launch(Dispatchers.IO) { memoInfoDao.deleteMemoInfoByIdDao(id) }
}

package com.example.samplenotepad.data

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import arrow.core.extensions.list.semigroup.plus
import arrow.core.getOrElse
import arrow.core.k
import arrow.core.toOption
import com.example.samplenotepad.R
import com.example.samplenotepad.entities.*
import com.example.samplenotepad.viewModels.MemoEditViewModel
import com.example.samplenotepad.viewModels.SearchViewModel
import com.example.samplenotepad.views.SampleMemoApplication
import com.example.samplenotepad.views.main.MemoOptionFragment
import kotlinx.coroutines.*
import java.io.File


private val showMassageForSavedLiveData: MutableLiveData<WhichFragment> = MutableLiveData()

internal fun getShowMassageForSavedLiveData() = showMassageForSavedLiveData

internal fun resetValueOfShowMassageForSavedLiveData() = showMassageForSavedLiveData.postValue(NoneOfThem)

//Database挿入時のシリアライズ処理
internal fun MemoContents.serializeMemoContents(): String {
    val stBuilder = StringBuilder()

    this.map { stBuilder.append(
        ":${it.memoRowId.value},${it.text.value},${it.checkBoxId.value.orNull()}," +
                "${it.checkBoxState.value},${it.dotId.value.orNull()}"
    ) }

    //完成したstBuilderから最初の「:」をdropしてリターン
    return stBuilder.drop(1).toString()
}

//Databaseから取得する時のデシリアライズ処理
internal fun String.deserializeMemoContents(): MemoContents {
    //StringをmemoRowInfo(List<String>)ごとのListにする
    val stringListOfList: List<List<String>> = this.split(":").map { it.split(",") }

    return stringListOfList.flatMap { stringList ->
        mutableListOf<MemoRowInfo>().apply { add(MemoRowInfo(
            MemoRowId(stringList[0].toInt()),
            Text(stringList[1]),
            CheckBoxId(stringList[2].toIntOrNull().toOption()),
            CheckBoxState(stringList[3].toBoolean()),
            DotId(stringList[4].toIntOrNull().toOption())
        ) ) }
    }.k()
}

private fun MemoContents.createContentsText(): String {
    val builder = StringBuilder("")

    this.toList().onEach { memoRowInfo ->
        builder.append(memoRowInfo.text.value + '\u21B5')
    }

    return builder.toString()
}

private fun MemoInfo.saveMemoInfoToDatabaseAsync(viewModel: ViewModel) = runBlocking {
    launch(Dispatchers.IO) {
        val memoInfoDao = AppDatabase.getDatabase(SampleMemoApplication.instance).memoInfoDao()

        //新規のメモはMemoInfoTableに挿入。その他はtableにアップデート。
        when (this@saveMemoInfoToDatabaseAsync.rowid) {
            0L -> {
                //databaseに新しいmemoInfoを挿入
                val rowId = memoInfoDao.insertMemoInfo(this@saveMemoInfoToDatabaseAsync)
                Log.d("場所:saveMemoInfo#挿入", "NewMemoId=${rowId} MemoContents=${memoInfoDao.getMemoInfoById(rowId).contents.deserializeMemoContents()}")

                updateMemoInfoAndMemoContentsAtSavePointInViewModel(viewModel, rowId)
            }
            else -> {
                //databaseに編集したmemoInfoをアップデート
                memoInfoDao.updateMemoInfo(this@saveMemoInfoToDatabaseAsync)
                Log.d("場所:saveMemoInfo#アップデート", "MemoId=${this@saveMemoInfoToDatabaseAsync.rowid} MemoContents=${memoInfoDao.getMemoInfoById(this@saveMemoInfoToDatabaseAsync.rowid).contents.deserializeMemoContents()}")

                updateMemoInfoAndMemoContentsAtSavePointInViewModel(viewModel, null)
            }
        }
    }
}

private fun MemoInfo.updateMemoInfoAndMemoContentsAtSavePointInViewModel(
    viewModel: ViewModel,
    memoId: Long?
) {
    val memoInfo = if (memoId != null) this.copy(rowid = memoId) else this

    when (viewModel) {
        is MemoEditViewModel -> {
            viewModel.apply {
                updateMemoInfo { memoInfo }
                updateMemoContentsAtSavePoint()
            }
        }
        is SearchViewModel -> {
            viewModel.apply {
                updateMemoInfo { memoInfo }
                updateMemoContentsAtSavePoint()
            }
        }
    }
}

internal fun saveMemoInfo(
    executionType: WhichMemoExecution,
    viewModel: ViewModel,
    memoInfo: MemoInfo?,
    memoContents: MemoContents
) = runBlocking {
    Log.d("saveMemoInfo", "saveMemoInfoに入った")

    val stringMemoContents = async(Dispatchers.Default) { memoContents.serializeMemoContents() }
    val contentsText = async(Dispatchers.Default) { memoContents.createContentsText() }
    val optionValues = MemoOptionFragment.getOptionValuesForSave()
    Log.d("saveMemoInfo", "optionValues=${optionValues}")

    val newMemoInfo = MemoInfo(
        memoInfo?.rowid ?: 0,
        System.currentTimeMillis(),
        optionValues.title.getOrElse {
            SampleMemoApplication.instance.getString(R.string.memo_title_default_value)
        },
        optionValues.category.getOrElse {
            SampleMemoApplication.instance.getString(R.string.memo_category_default_value)
        },
        stringMemoContents.await(),
        contentsText.await(),
        optionValues.targetDate.getOrElse { null },
        optionValues.targetTime.getOrElse { null },
        optionValues.preAlarm.getOrElse { 0 },
        optionValues.postAlarm.getOrElse { 0 }
    )

    Log.d("場所:saveMemoInfo#NewMemoInfo", "MemoId=${newMemoInfo.rowid} MemoContents=${newMemoInfo.contents.deserializeMemoContents()}")

    newMemoInfo.saveMemoInfoToDatabaseAsync(viewModel).join()

    when (executionType) {
        is DisplayExistMemo -> showMassageForSavedLiveData.postValue(DisplayFragment)
        else -> showMassageForSavedLiveData.postValue(EditFragment)
    }
}

internal fun updateMemoContentsInDatabase(
    executionType: WhichMemoExecution,
    memoId: Long,
    memoContents: MemoContents
) = runBlocking {
     launch(Dispatchers.IO) {
        val memoInfoDao = AppDatabase.getDatabase(SampleMemoApplication.instance).memoInfoDao()
        val newContents = memoContents.serializeMemoContents()
        val newContentsText = memoContents.createContentsText()
        val timeStamp = System.currentTimeMillis()
        Log.d("場所:updateMemoContentsInDatabase#Update前", "MemoId=$memoId MemoContents=$memoContents")

        memoInfoDao.updateContents(memoId, timeStamp, newContents, newContentsText)

        when (executionType) {
            is CreateNewMemo -> showMassageForSavedLiveData.postValue(EditFragment)
            is DisplayExistMemo -> showMassageForSavedLiveData.postValue(DisplayFragment)
        }

        Log.d("場所:updateMemoContentsInDatabase#Update後", "MemoId=$memoId MemoContents=${loadMemoInfoFromDatabase(memoId).contents.deserializeMemoContents()}")
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
    val stringData = template.serializeMemoContents()

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
            true -> SampleMemoApplication.instance.openFileInput(MEMO_TEMPLATE_FILE + templateName)
                .bufferedReader().readLine().deserializeMemoContents()
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

internal fun loadDataSetForEachMemoListFromDatabase(
    category: String
): List<DataSetForEachMemoList> = runBlocking {
    withContext(Dispatchers.IO) {
        val memoInfoDao = AppDatabase.getDatabase(SampleMemoApplication.instance).memoInfoDao()

        memoInfoDao.getDataSetForEachMemoList(category)
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

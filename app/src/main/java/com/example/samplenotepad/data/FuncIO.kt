package com.example.samplenotepad.data

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import arrow.core.*
import arrow.core.extensions.list.alternative.some
import arrow.core.extensions.list.semigroup.plus
import com.example.samplenotepad.R
import com.example.samplenotepad.entities.*
import com.example.samplenotepad.viewModels.MemoEditViewModel
import com.example.samplenotepad.viewModels.MemoOptionViewModel.Companion.getOptionValuesForSave
import com.example.samplenotepad.viewModels.SearchViewModel
import com.example.samplenotepad.views.main.MEMO_TEMPLATE_FILE
import com.example.samplenotepad.views.main.MEMO_TEMPLATE_NAME_LIST_FILE
import com.example.samplenotepad.views.main.MemoEditFragment
import com.example.samplenotepad.views.main.showSnackbarForSavedMassage
import com.example.samplenotepad.views.search.DisplayMemoFragment
import com.example.samplenotepad.views.search.MemoSearchActivity
import kotlinx.coroutines.*


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

private fun MemoInfo.saveMemoInfoToDatabaseAsync(fragment: Fragment, viewModel: ViewModel) = runBlocking {
    launch(Dispatchers.IO) {
        val memoInfoDao = AppDatabase.getDatabase(fragment.requireContext()).memoInfoDao()

        //新規のメモはMemoInfoTableに挿入。その他はtableにアップデート。
        when (this@saveMemoInfoToDatabaseAsync.rowid) {
            0L -> {
                //databaseに新しいmemoInfoを挿入
                val rowId = memoInfoDao.insertMemoInfo(this@saveMemoInfoToDatabaseAsync)
                Log.d("場所:saveMemoInfo#挿入", "NewMemoId=${rowId} MemoContents=${memoInfoDao.getMemoInfoById(rowId).contents.deserializeMemoContents()}")

                updateMemoInfoAndMemoContentsAtSavePointInViewModel(fragment, viewModel, rowId)
            }
            else -> {
                //databaseに編集したmemoInfoをアップデート
                memoInfoDao.updateMemoInfo(this@saveMemoInfoToDatabaseAsync)
                Log.d("場所:saveMemoInfo#アップデート", "MemoId=${this@saveMemoInfoToDatabaseAsync.rowid} MemoContents=${memoInfoDao.getMemoInfoById(this@saveMemoInfoToDatabaseAsync.rowid).contents.deserializeMemoContents()}")

                updateMemoInfoAndMemoContentsAtSavePointInViewModel(fragment, viewModel, null)
            }
        }
    }
}

private fun MemoInfo.updateMemoInfoAndMemoContentsAtSavePointInViewModel(
    fragment: Fragment,
    viewModel: ViewModel,
    memoId: Long?
) {
    val memoInfo = if (memoId != null) this.copy(rowid = memoId) else this

    when (fragment) {
        is MemoEditFragment -> {
            (viewModel as MemoEditViewModel).apply {
                updateMemoInfo { memoInfo }
                updateMemoContentsAtSavePoint()
            }
        }
        is DisplayMemoFragment -> {
            (viewModel as SearchViewModel).apply {
                updateMemoInfo { memoInfo }
                updateMemoContentsAtSavePoint()
            }
        }
    }
}

internal fun saveMemoInfo(
    fragment: Fragment,
    viewModel: ViewModel,
    memoInfo: MemoInfo?,
    memoContents: MemoContents
) = runBlocking {
    Log.d("saveMemoInfo", "saveMemoInfoに入った")

    val stringMemoContents = async(Dispatchers.Default) { memoContents.serializeMemoContents() }
    val contentsText = async(Dispatchers.Default) { memoContents.createContentsText() }
    val optionValues = getOptionValuesForSave()
    Log.d("saveMemoInfo", "optionValues=${optionValues}")

    val newMemoInfo = MemoInfo(
        memoInfo?.rowid ?: 0,
        System.currentTimeMillis(),
        optionValues.title.getOrElse { fragment.getString(R.string.memo_title_default_value) },
        optionValues.category.getOrElse { fragment.getString(R.string.memo_category_default_value) },
        stringMemoContents.await(),
        contentsText.await(),
        optionValues.targetDate.getOrElse { null },
        optionValues.targetTime.getOrElse { null },
        optionValues.preAlarm.getOrElse { null },
        optionValues.postAlarm.getOrElse { null }
    )

    Log.d("場所:saveMemoInfo#NewMemoInfo", "MemoId=${newMemoInfo.rowid} MemoContents=${newMemoInfo.contents.deserializeMemoContents()}")

    newMemoInfo.saveMemoInfoToDatabaseAsync(fragment, viewModel).join()

    showSnackbarForSavedMassage(fragment)
}

internal fun updateMemoContentsInDatabase(
    fragment: Fragment,
    memoId: Long,
    memoContents: MemoContents
) = runBlocking {
    withContext(fragment.lifecycleScope.coroutineContext + Dispatchers.IO) {
        val memoInfoDao = AppDatabase.getDatabase(fragment.requireContext()).memoInfoDao()
        val newContents = memoContents.serializeMemoContents()
        val newContentsText = memoContents.createContentsText()
        val timeStamp = System.currentTimeMillis()
        Log.d("場所:updateMemoContentsInDatabase#Update前", "MemoId=$memoId MemoContents=$memoContents")

        memoInfoDao.updateContents(memoId, timeStamp, newContents, newContentsText)

        showSnackbarForSavedMassage(fragment)

        Log.d("場所:updateMemoContentsInDatabase#Update後", "MemoId=$memoId MemoContents=${loadMemoInfoFromDatabase(fragment.requireActivity(), memoId).contents.deserializeMemoContents()}")
    }
}

internal fun saveTemplateNameListToFile(context: Context, templateNameList: List<String>) = runBlocking {
    val stringData = templateNameList.joinToString(separator = ",")

    launch(Dispatchers.IO) {
        context.openFileOutput(MEMO_TEMPLATE_NAME_LIST_FILE, Context.MODE_PRIVATE).use {
            it.write(stringData.toByteArray())
        }
    }
}

internal fun loadTemplateNameListFromFile(fragment: Fragment): List<String> = runBlocking {
    val file = fragment.requireActivity().getFileStreamPath(MEMO_TEMPLATE_NAME_LIST_FILE)

    withContext(Dispatchers.IO) {
        when (file.exists()) {
            true -> fragment.requireContext().openFileInput(MEMO_TEMPLATE_NAME_LIST_FILE)
                .bufferedReader().readLine().split(",")
            false -> listOf<String>()
        }
    }
}

internal fun saveTemplateToFile(
    context: Context,
    templateName: String,
    template: MemoContents
) = runBlocking {
    val stringData = template.serializeMemoContents()

    launch(Dispatchers.IO) {
        context.openFileOutput(MEMO_TEMPLATE_FILE + templateName, Context.MODE_PRIVATE).use {
            it.write(stringData.toByteArray())
        }
    }
}

internal fun loadTemplateFromFile(fragment: Fragment, templateName: String): MemoContents = runBlocking {
    val file = fragment.requireActivity().getFileStreamPath(MEMO_TEMPLATE_FILE + templateName)

    withContext(Dispatchers.IO) {
        when (file.exists()) {
            true -> fragment.requireContext().openFileInput(MEMO_TEMPLATE_FILE + templateName)
                .bufferedReader().readLine().deserializeMemoContents()
            false -> throw(IllegalArgumentException("File does not exist for「$templateName」"))
        }
    }
}

internal fun deleteTemplateFile(fragment: Fragment, templateName: String) {
    val file = fragment.requireActivity().getFileStreamPath(MEMO_TEMPLATE_FILE + templateName)

    file.delete()
}


internal fun loadMemoInfoFromDatabase(activity: Activity, memoInfoId: Long): MemoInfo = runBlocking {
    withContext(Dispatchers.IO) {
        val memoInfoDao = AppDatabase.getDatabase(activity).memoInfoDao()

        memoInfoDao.getMemoInfoById(memoInfoId)
    }
}

internal fun loadCategoryListFromDatabase(fragment: MemoEditFragment): List<String> = runBlocking {
    val categoryList = withContext(fragment.lifecycleScope.coroutineContext + Dispatchers.IO) {
        AppDatabase.getDatabase(fragment.requireContext()).memoInfoDao().getCategoryList()
    }
    val defaultCategoryName = fragment.getString(R.string.memo_category_default_value)

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
internal fun loadDataSetForCategoryListFromDatabase(
    activity: MemoSearchActivity
): List<DataSetForCategoryList> = runBlocking {
    withContext(activity.lifecycleScope.coroutineContext + Dispatchers.IO) {
        val memoInfoDao = AppDatabase.getDatabase(activity.applicationContext).memoInfoDao()
        val defaultCategoryName = activity.getString(R.string.memo_category_default_value)
        val list = memoInfoDao.getDataSetForCategoryList()
        Log.d("場所:loadCategoryDataSetForSearchTop#true", "list=$list")

        //defaultCategoryがListに含まれているかの条件分けと、defaultCategoryをListの先頭にする処理
        when (list.firstOrNull { it.name == defaultCategoryName } == null) {
            true -> {
                val sortedList = listOf(DataSetForCategoryList(defaultCategoryName, 0)).plus(list)
                Log.d("場所:loadCategoryDataSetForSearchTop#true", "sortedList=$sortedList")
                sortedList
            }
            false -> {
                val targetIndex= list.indexOfFirst { it.name == defaultCategoryName }
                val targetValue = list[targetIndex]
                val sortedList = listOf(targetValue).plus(list.filterNot { it.name == defaultCategoryName })

                Log.d("場所:loadCategoryDataSetForSearchTop#false", "sortedList=$sortedList")
                sortedList
            }
        }
    }
}

internal fun loadDataSetForEachMemoListFromDatabase(
    fragment: Fragment,
    category: String
): List<DataSetForEachMemoList> = runBlocking {
    withContext(fragment.lifecycleScope.coroutineContext + Dispatchers.IO) {
        val memoInfoDao = AppDatabase.getDatabase(fragment.requireContext()).memoInfoDao()

        memoInfoDao.getDataSetForEachMemoList(category)
    }
}

internal fun deleteMemoByCategoryFromDatabase(fragment: Fragment, category: String) = runBlocking {
    val memoInfoDao = AppDatabase.getDatabase(fragment.requireContext()).memoInfoDao()

    launch(Dispatchers.IO) { memoInfoDao.deleteByCategory(category) }
}

internal fun deleteMemoByIdFromDatabase(fragment: Fragment, id: Long) = runBlocking {
    val memoInfoDao = AppDatabase.getDatabase(fragment.requireContext()).memoInfoDao()

    launch(Dispatchers.IO) { memoInfoDao.deleteById(id) }
}

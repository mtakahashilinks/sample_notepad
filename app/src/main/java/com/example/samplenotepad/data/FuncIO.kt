package com.example.samplenotepad.data

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import arrow.core.*
import arrow.core.extensions.list.semigroup.plus
import com.example.samplenotepad.R
import com.example.samplenotepad.entities.*
import com.example.samplenotepad.viewModels.MemoEditViewModel
import com.example.samplenotepad.viewModels.MemoOptionViewModel.Companion.getOptionValuesForSave
import com.example.samplenotepad.views.main.MemoEditFragment
import com.example.samplenotepad.views.main.showSnackbarForSavedAtDisplayFragment
import com.example.samplenotepad.views.main.showSnackbarForSavedAtEditFragment
import com.example.samplenotepad.views.search.DisplayMemoFragment
import com.example.samplenotepad.views.search.MemoSearchActivity
import kotlinx.coroutines.*


const val MEMO_TEMPLATES_FILE = "memo_template_list"


//Database挿入時のシリアライズ処理
internal fun serializeMemoContents(memoContents: ListK<MemoRowInfo>): String {
    fun Option<Int>.convertFromOption(): Int? = when (this) {
        is Some -> this.t
        is None -> null
    }

    val stBuilder = StringBuilder()

    memoContents.map { stBuilder.append(
        ":${it.memoRowId.value},${it.text.value},${it.checkBoxId.value.convertFromOption()}," +
                "${it.checkBoxState.value},${it.dotId.value.convertFromOption()}"
    ) }

    //完成したstBuilderから最初の「:」をdropしてリターン
    return stBuilder.drop(1).toString()
}

//Databaseから取得する時のデシリアライズ処理
internal fun deserializeMemoContents(value: String): MemoContents {
    fun String.convertToOption(): Option<Int> = when (this) {
        "null" -> None
        else -> Some(this.toInt())
    }

    fun List<List<String>>.fromStringToMemoContents(): MemoContents {
        return this.flatMap { stList ->
            mutableListOf<MemoRowInfo>().apply { add(MemoRowInfo(
                MemoRowId(stList[0].toInt()),
                Text(stList[1]),
                CheckBoxId(stList[2].convertToOption()),
                CheckBoxState(stList[3].toBoolean()),
                DotId(stList[4].convertToOption())
            ) ) }
        }.k()
    }

    return value.split(":").map { it.split(",") }.fromStringToMemoContents()
}


internal fun saveMemoInfo(
    fragment: Fragment,
    viewModel: ViewModel,
    memoInfo: MemoInfo?,
    memoContents: MemoContents
) = runBlocking {
    Log.d("saveMemoInfo", "saveMemoInfoに入った")

    tailrec fun createContentsText(memoContents: List<MemoRowInfo>,
                                   builder: StringBuilder = StringBuilder("")): String {
        return when {
            memoContents.isEmpty() -> builder.toString()
            else -> createContentsText(memoContents.drop(1), builder.append(memoContents[0].text.value))
        }
    }

    fun saveMemoInfoToDatabase(newMemoInfo: MemoInfo) =
        launch(Dispatchers.IO) {
            val memoInfoDao = AppDatabase.getDatabase(fragment.requireContext()).memoInfoDao()

            //新規のメモはMemoInfoTableに挿入。その他はtableにアップデート。
            when (memoInfo) {
                null -> {
                    //databaseに新しいmemoInfoを挿入
                    val rowId = memoInfoDao.insertMemoInfo(newMemoInfo)
                    Log.d("場所:saveMemoInfo", "MemoInfoTableに挿入")
                    Log.d("場所:saveMemoInfo", "idOfInsert=$rowId")
                }
                else -> {
                    //databaseに編集したmemoInfoをアップデート
                    memoInfoDao.updateMemoInfo(newMemoInfo)
                    Log.d("場所:saveMemoInfo", "MemoInfoTableにupdate")
                }
            }
        }

    val stringMemoContents = async(Dispatchers.Default) { serializeMemoContents(memoContents) }
    val contentsText = async(Dispatchers.Default) { createContentsText(memoContents.toList()) }
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

    val databaseJob = saveMemoInfoToDatabase(newMemoInfo)

    when (fragment) {
        is MemoEditFragment -> {
            (viewModel as MemoEditViewModel).apply {
                updateMemoContentsAtSavePoint()
                updateMemoInfo { newMemoInfo }
            }

            databaseJob.join()

            showSnackbarForSavedAtEditFragment(fragment)
        }
        is DisplayMemoFragment -> {
            (viewModel as MemoEditViewModel).apply {
                updateMemoContentsAtSavePoint()
                updateMemoInfo { newMemoInfo }
            }

            databaseJob.join()

            showSnackbarForSavedAtDisplayFragment(fragment)
        }
        else -> {}
    }
}


//internal fun saveTemplateExecution(context: Context, categoryList: List<String>) = runBlocking {
//    val dataForSave = categoryList.joinToString(separator = ",")
//
//    launch(Dispatchers.IO) {
//        context.openFileOutput(MEMO_TEMPLATES_FILE, Context.MODE_PRIVATE).use {
//            it.write(dataForSave.toByteArray())
//        }
//    }
//}


//internal fun loadTemplate(fragment: MemoEditFragment): List<String> = runBlocking {
//    val file = fragment.requireActivity().getFileStreamPath(MEMO_TEMPLATES_FILE)
//
//    withContext(Dispatchers.IO) {
//        when (file.exists()) {
//            true ->
//                fragment.requireContext().openFileInput(MEMO_TEMPLATES_FILE).bufferedReader().readLine().split(",")
//            false -> listOf(fragment.getString(R.string.memo_category_default_value))
//        }
//    }
//}

internal fun loadMemoInfoFromDatabase(fragment: Fragment, memoInfoId: Long): MemoInfo = runBlocking {
    withContext(fragment.lifecycleScope.coroutineContext + Dispatchers.IO) {
        val memoInfoDao = AppDatabase.getDatabase(fragment.requireContext()).memoInfoDao()

        memoInfoDao.getMemoInfoById(memoInfoId)
    }
}

internal fun loadCategoryListFromDatabase(fragment: MemoEditFragment): List<String> = runBlocking {
    val categoryList = withContext(Dispatchers.IO) {
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

        val list = memoInfoDao.getCategoriesAndSize()
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

        memoInfoDao.getDataSetInCategory(category)
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

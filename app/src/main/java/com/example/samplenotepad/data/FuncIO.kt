package com.example.samplenotepad.data

import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import arrow.core.*
import com.example.samplenotepad.entities.*
import com.example.samplenotepad.viewModels.MemoInputViewModel
import com.example.samplenotepad.views.main.MemoInputFragment
import com.example.samplenotepad.views.main.showSnackbarForSaved
import kotlinx.coroutines.*


const val CATEGORY_FILE = "category_list"


//Database挿入時のシリアライズ処理
internal fun serializeMemoContents(memoContents: ListK<MemoRowInfo>): String {
    fun convertFromOption(value: Option<Int>): Int? {
        return when (value) {
            is Some -> value.t
            is None -> null
        }
    }

    val stBuilder = StringBuilder()

    memoContents.map { stBuilder.append(
        ":${it.memoRowId.value},${it.text.value},${convertFromOption(it.checkBoxId.value)}," +
                "${it.checkBoxState.value},${convertFromOption(it.dotId.value)}"
    ) }

    //完成したstBuilderから最初の「:」をdropしてリターン
    return stBuilder.drop(1).toString()
}

//Databaseから取得する時のデシリアライズ処理
internal fun deserializeMemoContents(value: String): MemoContents {
    fun convertToOption(value: String): Option<Int> {
        return when (value) {
            "null" -> None
            else -> Some(value.toInt())
        }
    }

    tailrec fun stringToMemoContents(mList: MutableList<MemoRowInfo>,
                                     stList: List<List<String>>): MemoContents {
        return when {
            stList.isEmpty() -> mList.k()
            else -> stringToMemoContents(
                mList.apply {
                    add(MemoRowInfo(
                        MemoRowId(stList[0][0].toInt()),
                        Text(stList[0][1]),
                        CheckBoxId(convertToOption(stList[0][2])),
                        CheckBoxState(stList[0][3].toBoolean()),
                        DotId(convertToOption(stList[0][4]))
                    ) )
                }
                , stList.drop(1)
            )
        }
    }

    return stringToMemoContents(mutableListOf(), value.split(":").map { it.split(",") })
}


internal fun saveMemoInfo(fragment: MemoInputFragment,
                          inputViewModel: MemoInputViewModel,
                          optionValues: ValuesOfOptionSetting) = runBlocking {
    Log.d("saveMemoInfo", "saveMemoInfoに入った")

    tailrec fun createContentsText(memoContents: List<MemoRowInfo>,
                                   builder: StringBuilder = StringBuilder("")): String {
        return when {
            memoContents.isEmpty() -> builder.toString()
            else -> createContentsText(memoContents.drop(1), builder.append(memoContents[0].text.value))
        }
    }


    launch { saveCategoryList(fragment, inputViewModel, optionValues.category) }

    val memoContents = inputViewModel.memoContents.value
    val stringMemoContents = async(Dispatchers.Default) { serializeMemoContents(memoContents) }
    val contentsText = async(Dispatchers.Default) { createContentsText(memoContents.toList()) }
    val memoInfoId = inputViewModel.memoInfo.value?.rowid
    val appDatabase = AppDatabase.getDatabase(fragment.requireContext())
    val memoInfoDao = appDatabase.memoInfoDao()

    val mMemoInfo = MemoInfo(
        memoInfoId ?: 0,
        System.currentTimeMillis(),
        optionValues.title,
        optionValues.category,
        stringMemoContents.await(),
        contentsText.await(),
        optionValues.targetDate.getOrElse { null },
        optionValues.targetTime.getOrElse { null },
        optionValues.preAlarm.getOrElse { null },
        optionValues.postAlarm.getOrElse { null }
    )

    val databaseJob = launch(Dispatchers.IO) {
        when (memoInfoId) {
            null -> {
                val rowId = memoInfoDao.insertMemoInfo(mMemoInfo)
                Log.d("場所:saveMemoInfo", "indexOfInsert=$rowId")
            }
            else -> memoInfoDao.updateMemoInfo(mMemoInfo)
        }
    }

    inputViewModel.memoInfo.updateAndGet { mMemoInfo }

    databaseJob.join()

    showSnackbarForSaved(fragment)
}

private fun saveCategoryList(fragment: MemoInputFragment,
                             viewModel: MemoInputViewModel,
                             newCategory: String) = runBlocking {
    val updatedList = viewModel.categoryList.value.plus(listOf(newCategory)).distinct()
    val contentsForSave = updatedList.joinToString(separator = ",")

    launch(Dispatchers.IO) {
        fragment.requireContext().openFileOutput(CATEGORY_FILE, Context.MODE_PRIVATE).use {
            it.write(contentsForSave.toByteArray())
        }
    }

    viewModel.viewModelScope.launch(Dispatchers.Main) {
        viewModel.categoryList.updateAndGet { updatedList }
    }
}

internal fun CoroutineScope.loadCategoryList(fragment: MemoInputFragment): List<String> {
    val file = fragment.requireActivity().getFileStreamPath(CATEGORY_FILE)

    return when (file.exists()) {
        true -> fragment.requireContext().openFileInput(CATEGORY_FILE).bufferedReader().readLine().split(",")
        false -> listOf("その他")
    }
}
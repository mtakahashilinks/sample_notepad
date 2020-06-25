package com.example.samplenotepad.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import arrow.core.internal.AtomicRefW
import arrow.core.k
import com.example.samplenotepad.data.*
import com.example.samplenotepad.data.loadDataSetForEachMemoListFromDatabase
import com.example.samplenotepad.data.loadMemoInfoFromDatabase
import com.example.samplenotepad.data.renameCategory
import com.example.samplenotepad.data.updateMemoContentsInDatabase
import com.example.samplenotepad.entities.*
import kotlinx.coroutines.runBlocking


class SearchViewModel : ViewModel() {

    private val dataSetForCategoryList = AtomicRefW(listOf<DataSetForCategoryList>())
    private val dataSetForEachMemoList = AtomicRefW(listOf<DataSetForEachMemoList>())
    private val memoInfo = AtomicRefW(MemoInfo(-1, -1, "", "", "", "", -1, -1, -1, -1))
    private val memoContents = AtomicRefW(MemoContents(listOf<MemoRowInfo>().k()))
    private val memoContentsAtSavePoint = AtomicRefW(listOf<MemoRowInfo>().k())
    private lateinit var selectedCategory: String

    internal fun getDataSetForCategoryList() = dataSetForCategoryList.value

    internal fun updateDataSetForCategoryList(
        newValue: (List<DataSetForCategoryList>) -> List<DataSetForCategoryList>
    ) = dataSetForCategoryList.updateAndGet { newValue(dataSetForCategoryList.value) }

    internal fun renameItemInDataSetForCategoryListAndUpdateDatabase(
        oldCategoryName: String,
        newCategoryName: String
    ) {
        dataSetForCategoryList.updateAndGet { list -> list.map {
            if (it.name == oldCategoryName) DataSetForCategoryList(newCategoryName, it.listSize)
            else it
        } }

        renameCategory(oldCategoryName, newCategoryName)
    }


    internal fun getDataSetForEachMemoList() = dataSetForEachMemoList.value

    internal fun updateDataSetForEachMemoList(
        newValue: (List<DataSetForEachMemoList>) -> List<DataSetForEachMemoList>
    ) = dataSetForEachMemoList.updateAndGet { newValue(dataSetForEachMemoList.value) }


    internal fun getMemoInfo() = memoInfo.value

    internal fun updateMemoInfo( newValue: (MemoInfo) -> MemoInfo) =
        memoInfo.updateAndGet { newValue(memoInfo.value) }

    internal fun loadMemoInfoAndUpdateInViewModel(memoInfoId: Long): MemoInfo {
        val mMemoInfo = loadMemoInfoFromDatabase(memoInfoId)

        memoInfo.updateAndGet { mMemoInfo }

        return mMemoInfo
    }


    internal fun updateMemoContents(newValue: (MemoContents) -> MemoContents) =
        memoContents.updateAndGet { newValue(memoContents.value) }

    internal fun getMemoContents() = memoContents.value

    internal fun updateMemoContentsInDatabaseAndSavePoint(executionType: WhichMemoExecution) {
        updateMemoContentsInDatabase(executionType, memoInfo.value.rowid, memoContents.value)
        updateMemoContentsAtSavePoint()
    }

    internal fun updateMemoContentsAtSavePoint() =
        memoContentsAtSavePoint.updateAndGet { memoContents.value }

    internal fun compareMemoContentsWithSavePoint(): Boolean {
        return memoContents.value == memoContentsAtSavePoint.value
    }


    internal fun getSelectedCategory() = selectedCategory

    internal fun updateSelectedCategory(value: String) {
        selectedCategory = value
    }

    internal fun initViewModelForSearchTop() {
        val mDataSetForCategoryList = loadDataSetForCategoryListFromDatabase()
        dataSetForCategoryList.updateAndGet { mDataSetForCategoryList }
    }

    internal fun initViewModelForSearchEachMemo(category: String) = runBlocking {
        val mDataSetForEachMemoList = loadDataSetForEachMemoListFromDatabase(category)
        dataSetForEachMemoList.updateAndGet { mDataSetForEachMemoList }
    }


    override fun onCleared() {
        super.onCleared()
        Log.d("場所:SearchViewModel", "onClearedが呼ばれた viewModel=$this")
    }
}

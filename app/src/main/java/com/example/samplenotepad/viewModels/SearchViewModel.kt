package com.example.samplenotepad.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import arrow.core.internal.AtomicRefW
import arrow.core.k
import com.example.samplenotepad.data.*
import com.example.samplenotepad.data.loadDataSetForMemoListFromDatabase
import com.example.samplenotepad.data.loadMemoInfoFromDatabase
import com.example.samplenotepad.data.renameCategory
import com.example.samplenotepad.data.updateMemoContentsInDatabase
import com.example.samplenotepad.entities.*
import kotlinx.coroutines.runBlocking


class SearchViewModel : ViewModel() {

    private val dataSetForCategoryList = AtomicRefW(listOf<DataSetForCategoryList>())
    private val dataSetForMemoList = AtomicRefW(listOf<DataSetForMemoList>())
    private val memoInfo = AtomicRefW(MemoInfo(-1, -1, "", "", "", "", -1, -1, -1, -1))
    private val memoContents = AtomicRefW(MemoContents(listOf<MemoRowInfo>().k()))
    private val memoContentsAtSavePoint = AtomicRefW(listOf<MemoRowInfo>().k())
    private val searchWord = AtomicRefW("")

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

    internal fun loadDataSetForCategoryListAndSetPropertyInViewModel() {
        val mDataSetForCategoryList = loadDataSetForCategoryListFromDatabase()
        dataSetForCategoryList.updateAndGet { mDataSetForCategoryList }
    }


    internal fun getDataSetForMemoList() = dataSetForMemoList.value

    internal fun updateDataSetForMemoList(
        newValue: (List<DataSetForMemoList>) -> List<DataSetForMemoList>
    ) = dataSetForMemoList.updateAndGet { newValue(dataSetForMemoList.value) }

    internal fun loadDataSetForMemoListAndSetPropertyInViewModel(category: String) = runBlocking {
        val mDataSetForMemoList = loadDataSetForMemoListFromDatabase(category)
        dataSetForMemoList.updateAndGet { mDataSetForMemoList }
    }


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


    internal fun searchMemoInfoAndSetWordAndResultForSearchTop(searchWord: String): List<DataSetForMemoList> {
        val result = searchMemoInfoForSearchTopInDatabase(searchWord)

        this.searchWord.updateAndGet { searchWord }
        dataSetForMemoList.updateAndGet { result }

        return result
    }

    internal fun searchMemoInfoAndSetWordAndResultForSearchInACategory(
        category: String,
        searchWord: String
    ): List<DataSetForMemoList> {
        val result = searchMemoInfoForSearchInACategoryInDatabase(category, searchWord)

        this.searchWord.updateAndGet { searchWord }
        dataSetForMemoList.updateAndGet { result }

        return result
    }

        internal fun getSearchWord() = searchWord.value




    override fun onCleared() {
        super.onCleared()
        Log.d("場所:SearchViewModel", "onClearedが呼ばれた viewModel=$this")
    }
}

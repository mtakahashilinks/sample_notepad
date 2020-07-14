package com.example.samplenotepad.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.samplenotepad.data.*
import com.example.samplenotepad.data.loadDataSetForMemoListIO
import com.example.samplenotepad.data.loadMemoInfoIO
import com.example.samplenotepad.data.renameCategoryIO
import com.example.samplenotepad.entities.*
import com.example.samplenotepad.usecases.createMemoContentsExecuteActor
import com.example.samplenotepad.usecases.getMemoContentsExecuteActor
import com.example.samplenotepad.usecases.saveMemo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking


class SearchViewModel : ViewModel() {

    private var dataSetForCategoryList = listOf<DataSetForCategoryList>()
    private var dataSetForMemoList = listOf<DataSetForMemoList>()
    private var memoInfo = MemoInfo(-1, "", "", "", "", "", "", -1, -1)
    private var savePointOfMemoContents = listOf<MemoRowInfo>()
    private var searchWord = ""

    internal fun getDataSetForCategoryList() = dataSetForCategoryList

    internal fun createNewMemoContentsExecuteActor() = createMemoContentsExecuteActor(this)

    internal fun updateDataSetForCategoryList(
        newValue: (List<DataSetForCategoryList>) -> List<DataSetForCategoryList>
    ) = newValue(dataSetForCategoryList).apply { dataSetForCategoryList = this }

    internal fun renameItemInDataSetForCategoryListAndUpdateDatabase(
        oldCategoryName: String,
        newCategoryName: String
    ) {
        updateDataSetForCategoryList { list -> list.map {
            if (it.name == oldCategoryName) DataSetForCategoryList(newCategoryName, it.listSize)
            else it
        } }

        renameCategoryIO(oldCategoryName, newCategoryName)
    }

    internal fun loadDataSetForCategoryListAndSetInViewModel() {
        updateDataSetForCategoryList { loadDataSetForCategoryListIO() }
    }


    internal fun getDataSetForMemoList() = dataSetForMemoList

    internal fun updateDataSetForMemoList(
        newValue: (List<DataSetForMemoList>) -> List<DataSetForMemoList>
    ) = newValue(dataSetForMemoList).apply { dataSetForMemoList = this }

    internal fun loadDataSetForMemoListAndSetInViewModel(category: String) {
        loadDataSetForMemoListIO(category).apply { dataSetForMemoList = this }
    }


    internal fun getMemoInfo() = memoInfo

    internal fun updateMemoInfo( newValue: (MemoInfo) -> MemoInfo) =
        newValue(memoInfo).apply { memoInfo = this }

    internal fun loadMemoInfoAndUpdateInViewModel(memoInfoId: Long): MemoInfo =
        loadMemoInfoIO(memoInfoId).apply { memoInfo = this }

    internal fun updateMemoInfoDatabase() = saveMemo(DisplayExistMemo)

    internal fun updateSavePointOfMemoContents() = runBlocking {
        val memoContentsDefer = CompletableDeferred<MemoContents>()
        getMemoContentsExecuteActor().send(GetMemoContents(memoContentsDefer))

        savePointOfMemoContents = memoContentsDefer.await()
    }

    internal fun isSavedAlready(): Boolean = runBlocking {
        val memoContentsDefer = CompletableDeferred<MemoContents>()
        getMemoContentsExecuteActor().send(GetMemoContents(memoContentsDefer))

        return@runBlocking memoContentsDefer.await() == savePointOfMemoContents
    }


    internal fun searchingMemoInfoAndSetValueInViewModel(searchWord: String): List<DataSetForMemoList> {
        this.searchWord = searchWord

        return updateDataSetForMemoList { searchMemoByASearchWordIO(searchWord) }
    }

    internal fun searchingMemoInfoAndSetValueInViewModel(
        category: String,
        searchWord: String
    ): List<DataSetForMemoList> {
        this.searchWord = searchWord

        return updateDataSetForMemoList { searchMemoByASearchWordAndCategoryIO(category, searchWord) }
    }

    internal fun getSearchWord() = searchWord



    override fun onCleared() {
        super.onCleared()
        Log.d("場所:SearchViewModel", "onClearedが呼ばれた viewModel=$this")
    }
}

package com.example.samplenotepad.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.samplenotepad.data.*
import com.example.samplenotepad.data.loadDataSetForMemoListByCategoryIO
import com.example.samplenotepad.data.renameCategoryIO
import com.example.samplenotepad.entities.*
import com.example.samplenotepad.views.SampleMemoApplication
import com.example.samplenotepad.usecases.createMemoContentsOperationActor
import com.example.samplenotepad.views.search.SearchTopFragment


class SearchViewModel : ViewModel() {

    private var dataSetForCategoryList = listOf<DataSetForCategoryList>()
    private var dataSetForMemoList = listOf<MemoInfo>()
    private var selectedCategoryInSearchTop = ""
    private var searchWord = ""

    internal fun createMemoContentsOperationActor() = createMemoContentsOperationActor(this)


    internal fun getDataSetForCategoryList() = dataSetForCategoryList

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

    internal fun loadAndSetDataSetForCategoryList() {
        updateDataSetForCategoryList { loadDataSetForCategoryListIO() }
    }


    internal fun getDataSetForMemoList() = dataSetForMemoList

    internal fun updateDataSetForMemoList(
        newValue: (List<MemoInfo>) -> List<MemoInfo>
    ) = newValue(dataSetForMemoList).apply { dataSetForMemoList = this }

    internal fun loadAndSetDataSetForMemoListFindByCategory() =
        loadDataSetForMemoListByCategoryIO(selectedCategoryInSearchTop).apply {
            dataSetForMemoList = this
        }


    internal fun loadAndSetDataSetForMemoListFindBySearchWord(
        searchWord: String
    ): List<MemoInfo> {
        this.searchWord = searchWord

        return updateDataSetForMemoList { loadMemoInfoListBySearchWordIO(searchWord) }
    }

    internal fun loadAndSetDataSetForMemoListFindBySearchWordAndCategory(
        searchWord: String
    ): List<MemoInfo> {
        this.searchWord = searchWord

        return updateDataSetForMemoList {
            loadMemoInfoListBySearchWordAndCategoryIO(selectedCategoryInSearchTop, searchWord)
        }
    }

    internal fun loadAndSetDataSetForMemoListFindByWithReminder(): List<MemoInfo> =
        updateDataSetForMemoList { loadMemoInfoListWithReminderIO() }

    internal fun loadAndSetDataSetForMemoListFindBySearchWordWithReminder(
        searchWord: String
    ): List<MemoInfo> {
        this.searchWord = searchWord

        return updateDataSetForMemoList { loadMemoInfoListBySearchWordWithReminderIO(searchWord) }
    }

    internal fun getSearchWord() = searchWord

    internal fun SearchTopFragment.setSelectedCategory(category: String) {
        selectedCategoryInSearchTop = category
    }

    internal fun MemoInfo.cancelAllAlarm(): MemoInfo {
        this.cancelAllAlarmIO(SampleMemoApplication.instance.baseContext)

        return this
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("場所:SearchViewModel", "onClearedが呼ばれた viewModel=$this")
    }
}

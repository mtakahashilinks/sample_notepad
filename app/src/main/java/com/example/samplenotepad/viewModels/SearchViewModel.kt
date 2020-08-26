package com.example.samplenotepad.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.samplenotepad.data.*
import com.example.samplenotepad.data.loadDataSetForMemoListByCategoryIO
import com.example.samplenotepad.data.renameCategoryInDatabaseIO
import com.example.samplenotepad.entities.*
import com.example.samplenotepad.views.SampleMemoApplication
import com.example.samplenotepad.usecases.createMemoContentsOperationActor


class SearchViewModel : ViewModel() {

    private var dataSetForCategoryList = listOf<DataSetForCategoryList>()
    private var dataSetForMemoList = listOf<MemoInfo>()
    private var selectedCategoryInSearchTop = ""
    private var searchWord = ""
    private var selectedDateOnCalendar =""

    internal fun createMemoContentsOperationActor() = createMemoContentsOperationActor(this)


    internal fun getDataSetForCategoryList() = dataSetForCategoryList

    internal fun updateDataSetForCategoryList(
        newValue: (List<DataSetForCategoryList>) -> List<DataSetForCategoryList>
    ) = newValue(dataSetForCategoryList).apply { dataSetForCategoryList = this }

    internal fun updateDataSetForCategoryListAndDatabaseForRename(
        oldCategoryName: String,
        newCategoryName: String
    ) {
        updateDataSetForCategoryList { list -> list.map {
            if (it.name == oldCategoryName) DataSetForCategoryList(newCategoryName, it.listSize)
            else it
        } }

        renameCategoryInDatabaseIO(oldCategoryName, newCategoryName)
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
        mSearchWord: String
    ): List<MemoInfo> {
        setSearchWord(mSearchWord)

        return updateDataSetForMemoList { loadMemoInfoListBySearchWordIO(mSearchWord) }
    }

    internal fun loadAndSetDataSetForMemoListFindBySearchWordAndCategory(
        mSearchWord: String
    ): List<MemoInfo> {
        setSearchWord(mSearchWord)

        return updateDataSetForMemoList {
            loadMemoInfoListBySearchWordAndCategoryIO(selectedCategoryInSearchTop, mSearchWord)
        }
    }

    internal fun loadAndSetDataSetForMemoListFindByWithReminder(): List<MemoInfo> =
        updateDataSetForMemoList { loadMemoInfoListWithReminderIO() }

    internal fun loadAndSetDataSetForMemoListFindBySearchWordWithReminder(
        mSearchWord: String
    ): List<MemoInfo> {
        setSearchWord(mSearchWord)

        return updateDataSetForMemoList { loadMemoInfoListBySearchWordWithReminderIO(mSearchWord) }
    }


    internal fun loadAndSetDataSetForMemoListFindBySearchWordAndDate(
        mSearchWord: String
    ): List<MemoInfo> {
        setSearchWord(mSearchWord)

        return updateDataSetForMemoList {
            loadMemoInfoListBySearchWordAndDateIO(mSearchWord, selectedDateOnCalendar)
        }
    }


    internal fun getSelectedCategory() = selectedCategoryInSearchTop

    internal fun setSelectedCategory(category: String) {
        selectedCategoryInSearchTop = category
    }


    internal fun getSearchWord() = searchWord

    internal fun setSearchWord(value: String) {
        searchWord = value
    }


    internal fun getSelectedDateOnCalendar() = selectedDateOnCalendar

    internal fun setSelectedDateOnCalendar(selectedDate: String) {
        selectedDateOnCalendar = selectedDate
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

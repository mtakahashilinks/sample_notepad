package com.example.samplenotepad.viewModels

import androidx.lifecycle.ViewModel
import arrow.core.internal.AtomicRefW
import arrow.core.k
import com.example.samplenotepad.entities.*


class SearchViewModel : ViewModel() {

    private val dataSetForCategoryList = AtomicRefW(listOf<DataSetForCategoryList>())
    private val dataSetForEachMemoList = AtomicRefW(listOf<DataSetForEachMemoList>())
    private val memoInfo = AtomicRefW(MemoInfo(-1, -1, "", "", "", "", -1, -1, -1, -1))
    private val memoContents = AtomicRefW(MemoContents(listOf<MemoRowInfo>().k()))
    private val memoContentsAtSavePoint = AtomicRefW(listOf<MemoRowInfo>().k())


    internal fun getDataSetForCategoryList() = dataSetForCategoryList.value
    internal inline fun updateDataSetForCategoryList(
        crossinline newValue: (List<DataSetForCategoryList>) -> List<DataSetForCategoryList>
    ) = dataSetForCategoryList.updateAndGet { newValue(dataSetForCategoryList.value) }

    internal fun getDataSetForEachMemoList() = dataSetForEachMemoList.value
    internal inline fun updateDataSetForEachMemoList(
        crossinline newValue: (List<DataSetForEachMemoList>) -> List<DataSetForEachMemoList>
    ) = dataSetForEachMemoList.updateAndGet { newValue(dataSetForEachMemoList.value) }

    internal fun getMemoInfo() = memoInfo.value
    internal inline fun updateMemoInfo(crossinline newValue: (MemoInfo) -> MemoInfo) =
        memoInfo.updateAndGet { newValue(memoInfo.value) }

    internal inline fun updateMemoContents(crossinline newValue: (MemoContents) -> MemoContents) =
        memoContents.updateAndGet { newValue(memoContents.value) }
    internal fun getMemoContents() = memoContents.value

    internal fun updateMemoContentsAtSavePoint() =
        memoContentsAtSavePoint.updateAndGet { memoContents.value }
    internal fun compareMemoContentsWithSavePoint(): Boolean {
        return memoContents.value == memoContentsAtSavePoint.value
    }


    override fun onCleared() {
        super.onCleared()
    }
}

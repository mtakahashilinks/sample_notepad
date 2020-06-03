package com.example.samplenotepad.viewModels


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.ListK
import arrow.core.internal.AtomicBooleanW
import arrow.core.internal.AtomicRefW
import arrow.core.k
import com.example.samplenotepad.usecases.closeMemoContentsOperation
import com.example.samplenotepad.data.loadCategoryListFromDatabase
import com.example.samplenotepad.entities.MemoContents
import com.example.samplenotepad.entities.MemoInfo
import com.example.samplenotepad.entities.MemoRowInfo
import com.example.samplenotepad.views.main.MemoEditFragment
import kotlinx.android.synthetic.main.fragment_memo_edit.*
import kotlinx.coroutines.*


class MemoEditViewModel : ViewModel() {

    private val memoInfo: AtomicRefW<MemoInfo?> = AtomicRefW(null)
    private val memoContents = AtomicRefW(listOf<MemoRowInfo>().k())
    private val memoContentsAtSavePoint = AtomicRefW(listOf<MemoRowInfo>().k())
    private val ifAtFirstInText = AtomicBooleanW(false) //DelKeyが押された時にMemoRowの削除処理に入るかどうかの判断基準
    private val categoryList = AtomicRefW(listOf(""))
    private lateinit var templateList: ListK<String>
    private lateinit var editFragment: MemoEditFragment

    internal inline fun updateMemoInfo(crossinline newValue: (MemoInfo?) -> MemoInfo?) =
        memoInfo.updateAndGet { newValue(memoInfo.value) }
    internal fun getMemoInfo() = memoInfo.value

    internal inline fun updateMemoContents(crossinline newValue: (MemoContents) -> MemoContents) =
        memoContents.updateAndGet { newValue(memoContents.value) }
    internal fun getMemoContents() = memoContents.value

    internal fun updateMemoContentsAtSavePoint() =
        memoContentsAtSavePoint.updateAndGet { memoContents.value }
    internal fun compareMemoContentsWithSavePoint(): Boolean {
        //まずFocusを外してmemoContentsのTextを更新してから比較する
        editFragment.memoContentsContainerLayout.focusedChild?.clearFocus()
        return memoContents.value == memoContentsAtSavePoint.value
    }

    internal fun updateIfAtFirstInText(newValue: Boolean) =
        ifAtFirstInText.compareAndSet(expect = !newValue, update = newValue)
    internal fun getIfAtFirstInText() = ifAtFirstInText.value

    internal inline fun updateCategoryList(crossinline newValue: (List<String>) -> List<String>) =
        categoryList.updateAndGet { newValue(categoryList.value) }
    internal fun getCategoryList() = categoryList.value


    internal fun initMainViewModel(fragment: MemoEditFragment) {
        editFragment = fragment

        loadAndSetCategoryList()
    }

    private fun loadAndSetCategoryList() =
        viewModelScope.launch {
            categoryList.updateAndGet { loadCategoryListFromDatabase(editFragment) }
        }


    override fun onCleared() {
        super.onCleared()
        //ここに終了処理を書く

        closeMemoContentsOperation()
    }
}

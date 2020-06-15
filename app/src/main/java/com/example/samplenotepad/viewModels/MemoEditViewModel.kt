package com.example.samplenotepad.viewModels


import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import arrow.core.internal.AtomicBooleanW
import arrow.core.internal.AtomicRefW
import arrow.core.k
import com.example.samplenotepad.usecases.closeMemoContentsOperation
import com.example.samplenotepad.data.loadCategoryListFromDatabase
import com.example.samplenotepad.data.loadTemplateFromFile
import com.example.samplenotepad.data.loadTemplateNameListFromFile
import com.example.samplenotepad.entities.*
import com.example.samplenotepad.usecases.clearAll
import com.example.samplenotepad.views.main.MemoEditFragment
import kotlinx.android.synthetic.main.fragment_memo_edit.*
import kotlinx.coroutines.*


class MemoEditViewModel : ViewModel() {
    companion object {
        private lateinit var instanceOfVM: MemoEditViewModel

        internal fun getInstanceOrCreateNewOne(): MemoEditViewModel =
            when (::instanceOfVM.isInitialized) {
                true -> instanceOfVM
                false -> {
                    val editViewModel =
                        ViewModelProvider.NewInstanceFactory().create(MemoEditViewModel::class.java)
                    instanceOfVM = editViewModel
                    editViewModel
                }
            }
    }


    private val memoInfo: AtomicRefW<MemoInfo?> = AtomicRefW(null)
    private val memoContents = AtomicRefW(listOf<MemoRowInfo>().k())
    private val memoContentsAtSavePoint = AtomicRefW(listOf<MemoRowInfo>().k())
    private val ifAtFirstInText = AtomicBooleanW(false) //DelKeyが押された時にMemoRowの削除処理に入るかどうかの判断基準
    private val categoryList = AtomicRefW(listOf<String>(""))
    private val templateNameList = AtomicRefW(listOf<String>())
    private lateinit var editFragment: MemoEditFragment

    internal fun getMemoInfo() = memoInfo.value
    internal inline fun updateMemoInfo(crossinline newValue: (MemoInfo?) -> MemoInfo?) =
        memoInfo.updateAndGet { newValue(memoInfo.value) }

    internal fun getMemoContents() = memoContents.value
    internal inline fun updateMemoContents(crossinline newValue: (MemoContents) -> MemoContents) =
        memoContents.updateAndGet { newValue(memoContents.value) }

    internal fun updateMemoContentsAtSavePoint() {
        Log.d("場所:updateMemoContentsAtSavePoint#1", "MemoContents=${memoContents.value}")
        val a = memoContentsAtSavePoint.updateAndGet { memoContents.value }
        Log.d("場所:updateMemoContentsAtSavePoint#2", "SavePoint=${memoContents.value}")
    }
    internal fun compareMemoContentsWithSavePoint(): Boolean {
        //まずFocusを外してmemoContentsのTextを更新してから比較する
        editFragment.memoContentsContainerLayout.clearFocus()
        Log.d("場所:compareMemoContentsWithSavePoint#1", "MemoContents=${memoContents.value}")
        Log.d("場所:compareMemoContentsWithSavePoint#2", "SavePoint=${memoContentsAtSavePoint.value}")
        return memoContents.value == memoContentsAtSavePoint.value
    }

    internal fun getIfAtFirstInText() = ifAtFirstInText.value
    internal fun updateIfAtFirstInText(newValue: Boolean) =
        ifAtFirstInText.compareAndSet(expect = !newValue, update = newValue)

    internal fun getCategoryList() = categoryList.value
    internal inline fun updateCategoryList(crossinline newValue: (List<String>) -> List<String>) =
        categoryList.updateAndGet { newValue(categoryList.value) }
    private fun MemoEditFragment.loadAndSetCategoryList() =
        viewModelScope.launch {
            categoryList.updateAndGet { loadCategoryListFromDatabase(this@loadAndSetCategoryList) }
        }

    internal fun loadTemplateAndUpdateMemoContents(fragment: Fragment, templateName: String) {
        updateMemoContents { loadTemplateFromFile(fragment, templateName) }
        updateMemoContentsAtSavePoint()
    }

    internal fun getTemplateNameList() = templateNameList.value
    internal inline fun updateTemplateNameList(crossinline newValue: (List<String>) -> List<String>) {
        templateNameList.updateAndGet { newValue(templateNameList.value) }
    }

    private fun MemoEditFragment.loadAndSetTemplateNameList() =
        viewModelScope.launch {
            templateNameList.updateAndGet { loadTemplateNameListFromFile(this@loadAndSetTemplateNameList) }
        }


    internal fun initEditViewModel(fragment: MemoEditFragment) {
        editFragment = fragment

        fragment.apply {
            loadAndSetCategoryList()
            loadAndSetTemplateNameList()
        }
    }

    internal fun resetEditStatesForCreateNewMemo() {
        editFragment.loadAndSetCategoryList()
        clearAll()
    }



    override fun onCleared() {
        super.onCleared()
        //ここに終了処理を書く

        closeMemoContentsOperation()
    }
}

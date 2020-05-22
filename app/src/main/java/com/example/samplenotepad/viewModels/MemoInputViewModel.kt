package com.example.samplenotepad.viewModels


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.ListK
import arrow.core.internal.AtomicBooleanW
import arrow.core.internal.AtomicRefW
import arrow.core.k
import com.example.samplenotepad.usecases.closeMemoContentsOperation
import com.example.samplenotepad.data.loadCategoryList
import com.example.samplenotepad.entities.MemoInfo
import com.example.samplenotepad.entities.MemoRowInfo
import com.example.samplenotepad.views.main.MemoInputFragment
import kotlinx.coroutines.*


class MemoInputViewModel : ViewModel() {

    internal val memoInfo: AtomicRefW<MemoInfo?> = AtomicRefW(null)
    internal val memoContents = AtomicRefW(listOf<MemoRowInfo>().k())
    internal val ifAtFirstInText = AtomicBooleanW(false) //DelKeyが押された時にMemoRowの削除処理に入るかどうかの判断基準
    internal val categoryList = AtomicRefW(listOf("その他"))
    private lateinit var formatList: ListK<String>
    private lateinit var inputFragment: MemoInputFragment


    internal fun initMainViewModel(fragment: MemoInputFragment) {
        inputFragment = fragment

        loadAndSetCategoryList()
    }

    private fun loadAndSetCategoryList() =
        viewModelScope.launch(Dispatchers.IO) {
            categoryList.updateAndGet { loadCategoryList(inputFragment) }
        }


    override fun onCleared() {
        super.onCleared()
        //ここに終了処理を書く

        closeMemoContentsOperation()
    }
}

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
import com.example.samplenotepad.views.MemoInputFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext


class MemoInputViewModel : ViewModel() {

    internal val memoInfo: AtomicRefW<MemoInfo?> = AtomicRefW(null)
    internal val memoContents = AtomicRefW(listOf<MemoRowInfo>().k())
    internal val ifAtFirstInText = AtomicBooleanW(false) //DelKeyが押された時にMemoRowの削除処理に入るかどうかの判断基準
    internal val categoryList: List<String> by lazy { loadAndSetCategoryList() }
    private lateinit var formatList: ListK<String>
    private lateinit var inputFragment: MemoInputFragment


    internal fun initMainViewModel(fragment: MemoInputFragment) {
        inputFragment = fragment
    }

    private fun loadAndSetCategoryList() = runBlocking {
        withContext(viewModelScope.coroutineContext + Dispatchers.IO) {
            loadCategoryList(inputFragment)
        }
    }

    override fun onCleared() {
        super.onCleared()
        //ここに終了処理を書く

        closeMemoContentsOperation()
    }
}

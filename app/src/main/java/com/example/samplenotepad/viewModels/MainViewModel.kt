package com.example.samplenotepad.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.samplenotepad.data.*
import com.example.samplenotepad.data.loadCategoryListIO
import com.example.samplenotepad.data.loadTemplateBodyIO
import com.example.samplenotepad.data.loadTemplateNameListIO
import com.example.samplenotepad.data.renameTemplateIO
import com.example.samplenotepad.data.saveTemplateNameListIO
import com.example.samplenotepad.entities.*
import com.example.samplenotepad.usecases.clearAll
import com.example.samplenotepad.usecases.createMemoContentsOperationActor
import com.example.samplenotepad.usecases.getMemoContentsOperationActor
import com.example.samplenotepad.usecases.updateText
import com.example.samplenotepad.views.main.MemoOptionFragment
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking


class MainViewModel : ViewModel() {

    private var memoInfo: MemoInfo? = null
    private var savePointOfMemoContents = listOf<MemoRowInfo>()
    private var isChangedStateInOptionFragment = false
    private var categoryList = listOf<String>()
    private var templateNameList = listOf<String>()

    internal fun getMemoInfo() = memoInfo

    internal fun updateMemoInfo(newValue: (MemoInfo?) -> MemoInfo?): MemoInfo? =
        newValue(memoInfo).apply { memoInfo = this }

    internal fun updateSavePointOfMemoContents() = runBlocking {
        val memoContentsDefer = CompletableDeferred<MemoContents>()
        getMemoContentsOperationActor().send(GetMemoContents(memoContentsDefer))

        savePointOfMemoContents = memoContentsDefer.await()
    }

    internal fun isSaved(): Boolean = runBlocking {
        //まずmemoContentsのTextを更新してから比較する
        updateText()

        val memoContentsDefer = CompletableDeferred<MemoContents>()
        getMemoContentsOperationActor().send(GetMemoContents(memoContentsDefer))

        return@runBlocking memoContentsDefer.await() == savePointOfMemoContents
                && !isChangedStateInOptionFragment
    }

    internal fun MemoOptionFragment.valueChanged() { isChangedStateInOptionFragment = true }

    internal fun clearIsChangedStatesFlagInOptionFragment() { isChangedStateInOptionFragment = false }


    internal fun getCategoryList() = categoryList

    private fun loadAndSetCategoryList() = loadCategoryListIO().apply { categoryList = this }


    internal fun loadTemplateAndUpdateMemoContents(templateName: String) = runBlocking {
        getMemoContentsOperationActor().send(SetMemoContents(loadTemplateBodyIO(templateName)))
        updateSavePointOfMemoContents()
    }


    internal fun getTemplateNameList() = templateNameList

    internal fun updateTemplateNameList(newValue: (List<String>) -> List<String>) =
        newValue(templateNameList).apply { templateNameList = this }

    internal fun addItemInTemplateNameListAndSaveTemplateFile(templateName: String) = runBlocking {
        val memoContentsDefer = CompletableDeferred<MemoContents>()
        getMemoContentsOperationActor().send(GetMemoContents(memoContentsDefer))
        val templateNameList = updateTemplateNameList { list -> list.plus(templateName) }

        saveTemplateNameListIO(templateNameList)
        saveTemplateBodyIO(templateName, memoContentsDefer.await())
    }

    internal fun renameItemInTemplateNameListAndTemplateFilesName(
        oldTemplateName: String,
        newTemplateName: String
    ) {
        val newTemplateList = updateTemplateNameList { list ->
            list.map { if (it == oldTemplateName) newTemplateName else it }
        }

        saveTemplateNameListIO(newTemplateList)
        renameTemplateIO(oldTemplateName, newTemplateName)
    }

    private fun loadAndSetTemplateNameList() =
        updateTemplateNameList { loadTemplateNameListIO() }


    internal fun createNewMemoContentsExecuteActor() = createMemoContentsOperationActor(this)

    internal fun initMainViewModel() {
        loadAndSetCategoryList()
        loadAndSetTemplateNameList()
    }

    internal fun initViewModelForExistMemo(memoId: Long): MemoInfo = runBlocking {
        val mMemoInfo = loadMemoInfoIO(memoId)
        val memoContents = mMemoInfo.contents.deserializeToMemoContents()

        memoInfo = mMemoInfo
        getMemoContentsOperationActor().send(SetMemoContents(memoContents))

        return@runBlocking mMemoInfo
    }

    internal fun initStatesForCreateNewMemo() {
        clearAll()
        memoInfo = null
        loadAndSetCategoryList()

        MemoOptionFragment.instance()?.initAllStatesInOptionFragment(this)
    }


    override fun onCleared() {
        super.onCleared()
        Log.d("場所:MemoMainViewModel", "onClearedが呼ばれた viewModel=$this")
    }
}

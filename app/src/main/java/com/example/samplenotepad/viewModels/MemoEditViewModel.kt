package com.example.samplenotepad.viewModels


import android.util.Log
import androidx.lifecycle.MutableLiveData
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
import com.example.samplenotepad.views.main.MemoOptionFragment
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json


class MemoEditViewModel : ViewModel() {

    private var memoInfo: MemoInfo? = null
    private var savePointOfMemoContents = listOf<MemoRowInfo>()
    private var categoryList = listOf<String>()
    private var templateNameList = listOf<String>()
    private val clearAllFocusInMemoContainerLiveData = MutableLiveData<Boolean>(false)

    internal fun getMemoInfo() = memoInfo

    internal fun updateMemoInfo(newValue: (MemoInfo?) -> MemoInfo?): MemoInfo? =
        newValue(memoInfo).apply { memoInfo = this }

    internal fun updateSavePointOfMemoContents() = runBlocking {
        val memoContentsDefer = CompletableDeferred<MemoContents>()
        getMemoContentsOperationActor().send(GetMemoContents(memoContentsDefer))

        savePointOfMemoContents = memoContentsDefer.await()
    }

    internal fun isSavedMemoContents(): Boolean = runBlocking {
        //まずFocusを外してmemoContentsのTextを更新してから比較する
        clearAllFocusInMemoContainerLiveData.postValue(true)
        val memoContentsDefer = CompletableDeferred<MemoContents>()
        getMemoContentsOperationActor().send(GetMemoContents(memoContentsDefer))

        return@runBlocking memoContentsDefer.await() == savePointOfMemoContents
    }


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

    internal fun getClearAllFocusInMemoContainerLiveData() = clearAllFocusInMemoContainerLiveData

    internal fun resetValueOfClearAllFocusInMemoContainerLiveData() {
        clearAllFocusInMemoContainerLiveData.postValue(false)
    }

    internal fun createNewMemoContentsExecuteActor() = createMemoContentsOperationActor(this)

    internal fun initEditViewModel() {
        loadAndSetCategoryList()
        loadAndSetTemplateNameList()
    }

    internal fun initViewModelForExistMemo(memoId: Long): MemoInfo = runBlocking {
        val mMemoInfo = loadMemoInfoIO(memoId)
        val memoContents = Json.parse(MemoRowInfo.serializer().list, mMemoInfo.contents)

        memoInfo = mMemoInfo
        getMemoContentsOperationActor().send(SetMemoContents(memoContents))

        return@runBlocking mMemoInfo
    }

    internal fun resetViewsAndStatesForCreateNewMemo() {
        clearAll()
        memoInfo = null
        loadAndSetCategoryList()

        MemoOptionFragment.instance()?.resetValueOfAllView(this)
    }


    override fun onCleared() {
        super.onCleared()
        Log.d("場所:MemoEditViewModel", "onClearedが呼ばれた viewModel=$this")
    }
}

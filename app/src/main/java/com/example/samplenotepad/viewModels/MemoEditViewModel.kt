package com.example.samplenotepad.viewModels


import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import arrow.core.internal.AtomicBooleanW
import arrow.core.k
import com.example.samplenotepad.data.*
import com.example.samplenotepad.data.loadCategoryListIO
import com.example.samplenotepad.data.loadTemplateFromFileIO
import com.example.samplenotepad.data.loadTemplateNameListFromFileIO
import com.example.samplenotepad.data.renameTemplateFileIO
import com.example.samplenotepad.data.saveTemplateNameListToFileIO
import com.example.samplenotepad.entities.*
import com.example.samplenotepad.usecases.clearAll
import com.example.samplenotepad.usecases.createMemoContentsExecuteActor
import com.example.samplenotepad.usecases.getMemoContentsExecuteActor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json


class MemoEditViewModel : ViewModel() {

    private var memoInfo: MemoInfo? = null
    private var savePointOfMemoContents = listOf<MemoRowInfo>().k()
    private val ifAtFirstInText = AtomicBooleanW(false) //DelKeyが押された時にMemoRowの削除処理に入るかどうかの判断基準
    private var categoryList = listOf<String>()
    private var templateNameList = listOf<String>()
    private val clearAllFocusInMemoContainerLiveData = MutableLiveData<Boolean>(false)

    internal fun getMemoInfo() = memoInfo

    internal fun updateMemoInfo(newValue: (MemoInfo?) -> MemoInfo?): MemoInfo? =
        newValue(memoInfo).apply { memoInfo = this }

    internal fun updateSavePointOfMemoContents() = runBlocking {
        val memoContentsDefer = CompletableDeferred<MemoContents>()
        getMemoContentsExecuteActor().send(GetMemoContents(memoContentsDefer))

        savePointOfMemoContents = memoContentsDefer.await()
    }

    internal fun isSavedAlready(): Boolean = runBlocking {
        //まずFocusを外してmemoContentsのTextを更新してから比較する
        clearAllFocusInMemoContainerLiveData.postValue(true)
        val memoContentsDefer = CompletableDeferred<MemoContents>()
        getMemoContentsExecuteActor().send(GetMemoContents(memoContentsDefer))

        return@runBlocking memoContentsDefer.await() == savePointOfMemoContents
    }


    internal fun getIfAtFirstInText() = ifAtFirstInText.value

    internal fun updateIfAtFirstInText(newValue: Boolean) =
        ifAtFirstInText.compareAndSet(expect = !newValue, update = newValue)


    internal fun getCategoryList() = categoryList

    internal fun updateCategoryList(newValue: (List<String>) -> List<String>) =
        newValue(categoryList).apply { categoryList = this }

    private fun loadAndSetCategoryList() = loadCategoryListIO().apply { categoryList = this }


    internal fun loadTemplateAndUpdateMemoContents(templateName: String) = runBlocking {
        getMemoContentsExecuteActor().send(SetMemoContents(loadTemplateFromFileIO(templateName)))
        updateSavePointOfMemoContents()
    }


    internal fun getTemplateNameList() = templateNameList

    internal fun updateTemplateNameList(newValue: (List<String>) -> List<String>) =
        newValue(templateNameList).apply { templateNameList = this }

    internal fun addItemInTemplateNameListAndSaveTemplateFile(templateName: String) = runBlocking {
        val memoContentsDefer = CompletableDeferred<MemoContents>()
        getMemoContentsExecuteActor().send(GetMemoContents(memoContentsDefer))
        val templateNameList = updateTemplateNameList { list -> list.plus(templateName) }

        saveTemplateNameListToFileIO(templateNameList)
        saveTemplateToFileIO(templateName, memoContentsDefer.await())
    }

    internal fun renameItemInTemplateNameListAndTemplateFilesName(
        oldTemplateName: String,
        newTemplateName: String
    ) {
        val newTemplateList = updateTemplateNameList { list ->
            list.map { if (it == oldTemplateName) newTemplateName else it }
        }

        saveTemplateNameListToFileIO(newTemplateList)
        renameTemplateFileIO(oldTemplateName, newTemplateName)
    }

    private fun loadAndSetTemplateNameList() =
        updateTemplateNameList { loadTemplateNameListFromFileIO() }

    internal fun getClearAllFocusInMemoContainerLiveData() = clearAllFocusInMemoContainerLiveData

    internal fun resetValueOfClearAllFocusInMemoContainerLiveData() {
        clearAllFocusInMemoContainerLiveData.postValue(false)
    }

    internal fun createNewMemoContentsExecuteActor() = createMemoContentsExecuteActor(this)

    internal fun initEditViewModel() {
        loadAndSetCategoryList()
        loadAndSetTemplateNameList()
    }

    internal fun initViewModelForExistMemo(memoId: Long): MemoInfo = runBlocking {
        val mMemoInfo = loadMemoInfoIO(memoId)
        val memoContents = Json.parse(MemoRowInfo.serializer().list, mMemoInfo.contents).k()

        memoInfo = mMemoInfo
        getMemoContentsExecuteActor().send(SetMemoContents(memoContents))

        return@runBlocking mMemoInfo
    }

    internal fun resetEditStatesForCreateNewMemo() {
        clearAll()
        memoInfo = null
        loadAndSetCategoryList()
    }


    override fun onCleared() {
        super.onCleared()
        //ここに終了処理を書く
    }
}

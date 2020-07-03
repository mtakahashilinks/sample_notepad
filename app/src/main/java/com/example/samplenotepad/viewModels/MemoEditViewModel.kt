package com.example.samplenotepad.viewModels


import android.util.Log
import androidx.lifecycle.ViewModel
import arrow.core.internal.AtomicBooleanW
import arrow.core.internal.AtomicRefW
import arrow.core.k
import com.example.samplenotepad.data.*
import com.example.samplenotepad.data.loadCategoryListFromDatabase
import com.example.samplenotepad.data.loadTemplateFromFile
import com.example.samplenotepad.data.loadTemplateNameListFromFile
import com.example.samplenotepad.data.renameTemplateFile
import com.example.samplenotepad.data.saveTemplateNameListToFile
import com.example.samplenotepad.usecases.closeMemoContentsOperation
import com.example.samplenotepad.entities.*
import com.example.samplenotepad.usecases.clearAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json


class MemoEditViewModel : ViewModel() {

    private val memoInfo: AtomicRefW<MemoInfo?> = AtomicRefW(null)
    private val memoContents = AtomicRefW(listOf<MemoRowInfo>().k())
    private val memoContentsAtSavePoint = AtomicRefW(listOf<MemoRowInfo>().k())
    private val ifAtFirstInText = AtomicBooleanW(false) //DelKeyが押された時にMemoRowの削除処理に入るかどうかの判断基準
    private val categoryList = AtomicRefW(listOf<String>(""))
    private val templateNameList = AtomicRefW(listOf<String>())
    private val clearAllFocusInMemoContainerFlow = MutableStateFlow(false)

    internal fun getMemoInfo() = memoInfo.value

    internal fun updateMemoInfo(newValue: (MemoInfo?) -> MemoInfo?) =
        memoInfo.updateAndGet { newValue(memoInfo.value) }


    internal fun getMemoContents() = memoContents.value

    internal fun updateMemoContents(newValue: (MemoContents) -> MemoContents) =
        memoContents.updateAndGet { newValue(memoContents.value) }


    internal fun updateMemoContentsAtSavePoint() {
        Log.d("場所:updateMemoContentsAtSavePoint#1", "MemoContents=${memoContents.value}")
        memoContentsAtSavePoint.updateAndGet { memoContents.value }
        Log.d("場所:updateMemoContentsAtSavePoint#2", "SavePoint=${memoContents.value}")
    }

    internal fun compareMemoContentsWithSavePoint(): Boolean {
        //まずFocusを外してmemoContentsのTextを更新してから比較する
        clearAllFocusInMemoContainerFlow.value = true
        Log.d("場所:compareMemoContentsWithSavePoint#1", "MemoContents=${memoContents.value}")
        Log.d("場所:compareMemoContentsWithSavePoint#2", "SavePoint=${memoContentsAtSavePoint.value}")
        return memoContents.value == memoContentsAtSavePoint.value
    }


    internal fun getIfAtFirstInText() = ifAtFirstInText.value

    internal fun updateIfAtFirstInText(newValue: Boolean) =
        ifAtFirstInText.compareAndSet(expect = !newValue, update = newValue)


    internal fun getCategoryList() = categoryList.value

    internal fun updateCategoryList(newValue: (List<String>) -> List<String>) =
        categoryList.updateAndGet { newValue(categoryList.value) }

    private fun loadAndSetCategoryList() =
        categoryList.updateAndGet { loadCategoryListFromDatabase() }


    internal fun loadTemplateAndUpdateMemoContents(templateName: String) {
        updateMemoContents { loadTemplateFromFile(templateName) }
        updateMemoContentsAtSavePoint()
    }


    internal fun getTemplateNameList() = templateNameList.value

    internal fun updateTemplateNameList(newValue: (List<String>) -> List<String>) =
        templateNameList.updateAndGet { newValue(templateNameList.value) }

    internal fun addItemInTemplateNameListAndSaveTemplateFile(templateName: String) {
        val templateNameList = updateTemplateNameList { list -> list.plus(templateName) }
        saveTemplateNameListToFile(templateNameList)
        saveTemplateToFile(templateName, getMemoContents())
    }

    internal fun renameItemInTemplateNameListAndTemplateFilesName(
        oldTemplateName: String,
        newTemplateName: String
    ) {
        val newTemplateList = templateNameList.updateAndGet { list ->
            list.map { if (it == oldTemplateName) newTemplateName else it }
        }

        saveTemplateNameListToFile(newTemplateList)
        renameTemplateFile(oldTemplateName, newTemplateName)
    }

    private fun loadAndSetTemplateNameList() =
        templateNameList.updateAndGet { loadTemplateNameListFromFile() }

    internal fun getClearAllFocusInMemoContainerFlow() = clearAllFocusInMemoContainerFlow

    internal fun resetValueOfClearAllFocusInMemoContainerFlow() {
        clearAllFocusInMemoContainerFlow.value = false
    }

    internal fun initEditViewModel() {
        loadAndSetCategoryList()
        loadAndSetTemplateNameList()
    }

    internal fun initViewModelForExistMemo(memoId: Long): MemoInfo {
        val mMemoInfo = loadMemoInfoFromDatabase(memoId)
        val mMemoContents = Json.parse(MemoRowInfo.serializer().list, mMemoInfo.contents).k()

        memoInfo.updateAndGet { mMemoInfo }
        memoContents.updateAndGet { mMemoContents }

        return mMemoInfo
    }

    internal fun resetEditStatesForCreateNewMemo() {
        clearAll()
        memoInfo.updateAndGet { null }
        loadAndSetCategoryList()
    }


    override fun onCleared() {
        super.onCleared()
        //ここに終了処理を書く

        closeMemoContentsOperation()
    }
}

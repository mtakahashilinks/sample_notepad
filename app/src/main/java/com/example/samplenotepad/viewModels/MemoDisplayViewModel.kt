package com.example.samplenotepad.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.samplenotepad.data.cancelAllAlarmIO
import com.example.samplenotepad.data.loadMemoInfoIO
import com.example.samplenotepad.entities.*
import com.example.samplenotepad.usecases.createMemoContentsOperationActor
import com.example.samplenotepad.usecases.getMemoContentsOperationActor
import com.example.samplenotepad.usecases.saveMemo
import com.example.samplenotepad.views.SampleMemoApplication
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking


class MemoDisplayViewModel : ViewModel() {

    private var memoInfo = MemoInfo(-1, "", "", "", "", "", "", -1, -1)
    private var savePointOfMemoContents = listOf<MemoRowInfo>()

    internal fun createNewMemoContentsExecuteActor() = createMemoContentsOperationActor(this)

    internal fun getMemoInfo() = memoInfo

    internal fun updateMemoInfo( newValue: (MemoInfo) -> MemoInfo) =
        newValue(memoInfo).apply { memoInfo = this }

    internal fun loadMemoInfoAndUpdate(memoInfoId: Long): MemoInfo =
        loadMemoInfoIO(memoInfoId).apply { memoInfo = this }

    internal fun saveMemoInfo() = saveMemo(DisplayExistMemo)

    internal fun updateSavePointOfMemoContents() = runBlocking {
        val memoContentsDefer = CompletableDeferred<MemoContents>()
        getMemoContentsOperationActor().send(GetMemoContents(memoContentsDefer))

        savePointOfMemoContents = memoContentsDefer.await()
    }

    internal fun isSavedMemoContents(): Boolean = runBlocking {
        val memoContentsDefer = CompletableDeferred<MemoContents>()
        getMemoContentsOperationActor().send(GetMemoContents(memoContentsDefer))

        return@runBlocking memoContentsDefer.await() == savePointOfMemoContents
    }

    internal fun MemoInfo.cancelAllAlarm(): MemoInfo {
        this.cancelAllAlarmIO(SampleMemoApplication.instance.baseContext)

        return this
    }


    override fun onCleared() {
        super.onCleared()
        Log.d("場所:MemoDisplayViewModel", "onClearedが呼ばれた viewModel=$this")
    }
}
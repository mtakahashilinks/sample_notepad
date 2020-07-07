package com.example.samplenotepad.usecases

import android.text.InputType
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.samplenotepad.*
import com.example.samplenotepad.data.saveMemoInfoIO
import com.example.samplenotepad.entities.*
import com.example.samplenotepad.entities.GetMemoContents
import com.example.samplenotepad.viewModels.MemoEditViewModel
import com.example.samplenotepad.viewModels.SearchViewModel
import com.example.samplenotepad.views.MemoEditText
import com.example.samplenotepad.views.SampleMemoApplication
import com.example.samplenotepad.views.main.*
import com.example.samplenotepad.views.main.setConstraintForFirstMemoEditText
import com.example.samplenotepad.views.main.setConstraintForNextMemoEditTextWithNoBelow
import com.example.samplenotepad.views.search.DisplayMemoFragment
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor


internal lateinit var firstMemoEditText: MemoEditText private set
private lateinit var editFragment: MemoEditFragment
private lateinit var displayFragment: DisplayMemoFragment
private lateinit var editViewModel: MemoEditViewModel
private lateinit var searchViewModel: SearchViewModel
private lateinit var memoContainer: ConstraintLayout
private lateinit var executeActor: SendChannel<TypeOfMemoContentsOperation>
private lateinit var formerMemoEditTextForExistMemo: MemoEditText //databaseから読みだした既存のメモの表示の際にのみ使う

private val showMassageForSavedLiveData = MutableLiveData<TypeOfFragment>(NoneOfThem)

internal fun getShowMassageForSavedLiveData() = showMassageForSavedLiveData

internal fun resetValueOfShowMassageForSavedLiveData() {
    showMassageForSavedLiveData.postValue(NoneOfThem)
}

internal fun getMemoContentsExecuteActor() = executeActor

internal fun createMemoContentsExecuteActor(viewModel: ViewModel) {
    executeActor = viewModel.viewModelScope.memoContentsOperationActor()
}

@ObsoleteCoroutinesApi
internal fun initMemoContentsOperation(
    fragment: Fragment,
    viewModel: ViewModel,
    container: ConstraintLayout,
    executionType: TypeOfMemoExecution
) = runBlocking {
    when (executionType){
        is CreateNewMemo -> {
            editFragment = fragment as MemoEditFragment
            editViewModel = viewModel as MemoEditViewModel
            memoContainer = container

            editViewModel.apply {
                getMemoContentsExecuteActor().send(SetMemoContents(listOf<MemoRowInfo>()))

                executeActor.send(CreateFirstMemoEditText(Text(""), CreateNewMemo))

                updateSavePointOfMemoContents()
            }
        }
        is EditExistMemo -> {
            editFragment = fragment as MemoEditFragment
            editViewModel = viewModel as MemoEditViewModel
            memoContainer = container

            val memoContentsDefer = CompletableDeferred<MemoContents>()
            executeActor.send(GetMemoContents(memoContentsDefer))

            createMemoRowsForExistMemo(executionType, memoContentsDefer.await())
        }
        is DisplayExistMemo -> {
            displayFragment = fragment as DisplayMemoFragment
            searchViewModel = viewModel as SearchViewModel
            memoContainer = container

            val memoContentsDefer = CompletableDeferred<MemoContents>()
            executeActor.send(GetMemoContents(memoContentsDefer))

            createMemoRowsForExistMemo(executionType, memoContentsDefer.await())
        }
    }
}

internal fun closeMemoContentsExecuteActor() = executeActor.close()

//ボタンがクリックされた時のcheckBox処理の入り口
internal fun MemoEditText.checkBoxOperation() = runBlocking {
    val memoContentsDefer = CompletableDeferred<MemoContents>()
    executeActor.send(GetMemoContents(memoContentsDefer))

    val memoContents = memoContentsDefer.await()
    val memoRowInfo = memoContents.first { it.memoEditTextId.value == this@checkBoxOperation.id }
    val checkBoxId = memoRowInfo.checkBoxId.value

    when {
        memoRowInfo.dotId.value != null -> {
            executeActor.send(DeleteDot(this@checkBoxOperation))
            executeActor.send(AddCheckBox(this@checkBoxOperation, CreateNewMemo))
        }
        checkBoxId != null -> executeActor.send(DeleteCheckBox(this@checkBoxOperation))
        else -> executeActor.send(AddCheckBox(this@checkBoxOperation, CreateNewMemo))
    }
}

//ボタンがクリックされた時のdot処理の入り口
internal fun MemoEditText.dotOperation() = runBlocking {
    val memoContentsDefer = CompletableDeferred<MemoContents>()

    executeActor.send(GetMemoContents(memoContentsDefer))

    val memoContents = memoContentsDefer.await()
    val memoRowInfo = memoContents.first { it.memoEditTextId.value == this@dotOperation.id }
    val dotId = memoRowInfo.dotId.value

    when {
        memoRowInfo.checkBoxId.value != null -> {
            executeActor.send(DeleteCheckBox(this@dotOperation))
            executeActor.send(AddDot(this@dotOperation, CreateNewMemo))
        }
        dotId != null -> executeActor.send(DeleteDot(this@dotOperation))
        else -> executeActor.send(AddDot(this@dotOperation, CreateNewMemo))
    }
}

internal fun clearAll() = runBlocking {
    Log.d("場所:clearAll", "ClearAll処理に入った")

    memoContainer.removeAllViews()

    editViewModel.viewModelScope.launch {
        executeActor.send(CreateFirstMemoEditText(Text(""), CreateNewMemo))
    }.join()

    editViewModel.updateSavePointOfMemoContents()
}

internal fun saveMemo(executionType: TypeOfMemoExecution) = runBlocking {
    //フォーカスを外しすことでupdateTextOfMemoRowInfoが呼ばれてTextプロパティが更新される
    memoContainer.clearFocus()

    executeActor.send(SaveMemoInfo(executionType))

    when (executionType) {
        is DisplayExistMemo -> {
            searchViewModel.updateSavePointOfMemoContents()
            showMassageForSavedLiveData.postValue(DisplayFragment)
        }
        else -> {
            editViewModel.updateSavePointOfMemoContents()
            showMassageForSavedLiveData.postValue(EditFragment)
        }
    }
}

private fun saveOperation(executeId: SaveMemoInfo, memoContents: MemoContents) {
    Log.d("saveOperation", "save処理に入った")
    when (executeId.executionType) {
        is DisplayExistMemo -> searchViewModel.getMemoInfo().saveMemoInfoIO(searchViewModel, memoContents)
        else -> editViewModel.getMemoInfo().saveMemoInfoIO(editViewModel, memoContents)
    }
}

private fun createMemoRowsForExistMemo(
    executionType: TypeOfMemoExecution,
    memoContents: MemoContents
) = runBlocking {
    suspend fun List<MemoRowInfo>.createFirstRow(): List<MemoRowInfo> {
        val targetMemoRowInfo = this@createFirstRow[0]

        executeActor.send(CreateFirstMemoEditText(targetMemoRowInfo.memoText, executionType, targetMemoRowInfo))

        return this.drop(1)
    }

    suspend fun List<MemoRowInfo>.createNextRow() {
        this.onEach { memoRowInfo ->
            executeActor.send(CreateNextMemoEditText(memoRowInfo.memoText, executionType, memoRowInfo))
        }
    }

    Log.d("場所:createMemoRowsForExistMemo", "memoContents=${memoContents.toList()}")

    //主要な処理
    when (executionType) {
        is DisplayExistMemo -> {
            memoContents.toList().createFirstRow().createNextRow()

            searchViewModel.updateSavePointOfMemoContents()
        }
        else -> {
            memoContents.toList().createFirstRow().createNextRow()

            editViewModel.updateSavePointOfMemoContents()
        }
    }
}

private fun MemoEditText.addCheckBoxAndDotForExistMemo(
    memoRowInfo: MemoRowInfo,
    executionType: TypeOfMemoExecution,
    memoContents:MemoContents
) {
    if (memoRowInfo.checkBoxId.value != null)
        addCheckBox(
            AddCheckBox(this, executionType, memoRowInfo.checkBoxId.value, memoRowInfo.checkBoxState.value),
            memoContents
        )

    if (memoRowInfo.dotId.value != null)
        addDot(AddDot(this, executionType, memoRowInfo.dotId.value), memoContents)
}


//近い将来、代替えのAPIに切り替わるらしい
@ObsoleteCoroutinesApi
private fun CoroutineScope.memoContentsOperationActor() =
    actor<TypeOfMemoContentsOperation> {
        var memoContents = listOf<MemoRowInfo>()

        for (msg in channel) {
            Log.d("場所:memoContentsOperationActor1", "memoContentsOperationActorに入った executeId=$msg")
            Log.d("場所:memoContentsOperationActor2", "変更前:size=${memoContents.size} memoContents=$memoContents")

            when (msg) {
                is UpdateTextOfMemoRowInfo -> memoContents = updateTextOfMemoRowInfo(msg, memoContents)
                is CreateFirstMemoEditText -> memoContents = createFirstMemoEditText(msg, memoContents)
                is CreateNextMemoEditText -> memoContents = createNextMemoEditText(msg, memoContents)
                is DeleteMemoRow -> memoContents = deleteMemoRow(msg, memoContents)
                is AddCheckBox -> memoContents = addCheckBox(msg, memoContents)
                is DeleteCheckBox -> memoContents = deleteCheckBox(msg, memoContents)
                is ChangeCheckBoxState ->
                    memoContents = updateCheckBoxStateOfMemoContents(msg, memoContents)
                is AddDot -> memoContents = addDot(msg, memoContents)
                is DeleteDot -> memoContents = deleteDot(msg, memoContents)
                is SaveMemoInfo -> saveOperation(msg, memoContents)
                is SetMemoContents -> memoContents = msg.memoContents
                is GetMemoContents -> msg.response.complete(memoContents)
            }

            Log.d("場所:memoContentsOperationActor3", "変更後:size=${memoContents.size} memoContents=$memoContents")
            Log.d("場所:memoContentsOperationActor4", "executeMemoOperationが終わった executeId=$msg")
        }
    }


private fun updateTextOfMemoRowInfo(
    executeId: UpdateTextOfMemoRowInfo,
    memoContents: MemoContents
): MemoContents = memoContents.flatMap { memoRowInfo ->
    if (memoRowInfo.memoEditTextId.value == executeId.memoEditText.id)
        listOf(memoRowInfo.copy(memoText = Text(executeId.memoEditText.text.toString())))
    else listOf(memoRowInfo)
}


private fun MemoEditText.setCommonLayoutParams() {
    layoutParams = ConstraintLayout.LayoutParams(
        ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT
    )
    background = resources.getDrawable(
        android.R.color.transparent, SampleMemoApplication.instance.theme
    )
    setPadding(4)
    isFocusable = true
    isFocusableInTouchMode = true
    inputType = InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE
    isSingleLine = false
}

private fun createNewMemoEditText(
    memoEditTextId: Int?, text : Text, executeType: TypeOfMemoExecution
): MemoEditText = when (executeType) {
        is CreateNewMemo -> {
            MemoEditText(editFragment.context, editViewModel, executeActor).apply {
                id = View.generateViewId()
                setCommonLayoutParams()
            }
        }
        is EditExistMemo -> {
            MemoEditText(editFragment.context, editViewModel, executeActor).apply {
                setCommonLayoutParams()
                id = memoEditTextId ?: throw(NullPointerException("memoEditTextId mast be not null"))
                setText(text.value)
            }
        }
        is DisplayExistMemo -> {
            MemoEditText(displayFragment.context, editViewModel, executeActor).apply {
                setCommonLayoutParams()
                id = memoEditTextId ?: throw(NullPointerException("memoEditTextId mast be not null"))
                setText(text.value)
                isFocusable = false
                isFocusableInTouchMode = false
            }
        }
    }

//MemoContentsの最初の行をセットする
private fun createFirstMemoEditText(
    executeId: CreateFirstMemoEditText,
    memoContents: MemoContents
): MemoContents {
    Log.d("場所:createFirstMemoEditText", "createFirstMemoEditTextに入った")

    val newMemoEditText = createNewMemoEditText(
        executeId.memoRowInfo?.memoEditTextId?.value, executeId.text, executeId.executionType
    )

    firstMemoEditText = newMemoEditText

    return when (executeId.executionType) {
        is CreateNewMemo -> {
            editViewModel.viewModelScope
                .launch(Dispatchers.Main) { memoContainer.setConstraintForFirstMemoEditText(newMemoEditText) }

            Log.d("場所:createFirstMemoEditText", "newMemoEditTextId=${newMemoEditText.id}")

            newMemoEditText.setFocusAndTextAndCursorPosition(executeId.text)

            listOf(MemoRowInfo(MemoEditTextId(newMemoEditText.id), executeId.text))
        }
        else -> {
            memoContainer.setConstraintForFirstMemoEditText(newMemoEditText)

            if (executeId.memoRowInfo != null) {
                newMemoEditText.addCheckBoxAndDotForExistMemo(
                    executeId.memoRowInfo, executeId.executionType, memoContents
                )
            }

            formerMemoEditTextForExistMemo = newMemoEditText

            memoContents
        }
    }
}

private fun createNextMemoEditText(
    executeId: CreateNextMemoEditText,
    memoContents: MemoContents
): MemoContents {
    Log.d("場所:createNextMemoEditText", "createNextMemoEditTextに入った")

    val newMemoEditText = createNewMemoEditText(
        executeId.memoRowInfo?.memoEditTextId?.value, executeId.text, executeId.executionType
    )

    return when (executeId.executionType) {
        is CreateNewMemo -> {
            val targetMemoEditTextId = memoContainer.findFocus().id
            val indexOfTargetMemoRowInfo =
                memoContents.indexOfFirst { it.memoEditTextId.value == targetMemoEditTextId }
            val maxIndexOfList = memoContents.size - 1

            when (indexOfTargetMemoRowInfo == maxIndexOfList) {
                 true-> {
                    Log.d("場所:createNextMemoEditText", "下に他のViewがない場合のLayoutの制約設定とMemoContentsのUpdate")
                    memoContainer.setConstraintForNextMemoEditTextWithNoBelow(
                        newMemoEditText,
                        MemoEditTextId(targetMemoEditTextId),
                        executeId.text
                    )

                    memoContents.plus(listOf(MemoRowInfo(MemoEditTextId(newMemoEditText.id))))
                }
                false -> {
                    Log.d("場所:createNextMemoEditText", "下に他のViewがある場合のLayoutの制約とMemoContentsのUpdate")
                    val nextMemoEditTextId = memoContents[indexOfTargetMemoRowInfo + 1].memoEditTextId
                    val prefixList = memoContents.take(indexOfTargetMemoRowInfo + 1)
                    val suffixList = memoContents.drop(indexOfTargetMemoRowInfo + 1)

                    memoContainer.setConstraintForNextMemoEditTextWithBelow(
                        newMemoEditText,
                        MemoEditTextId(targetMemoEditTextId),
                        nextMemoEditTextId,
                        executeId.text
                    )

                    prefixList
                        .plus(listOf(MemoRowInfo(MemoEditTextId(newMemoEditText.id))))
                        .plus(suffixList)
                }
            }
        }
        else -> {
            //既存のメモの表示(DisplayExistMemo)か編集(EditExistMemo)の場合
            memoContainer.setConstraintForNextMemoEditTextWithNoBelow(
                newMemoEditText,
                MemoEditTextId(formerMemoEditTextForExistMemo.id),
                executeId.text
            )

            if (executeId.memoRowInfo?.checkBoxId != null || executeId.memoRowInfo?.dotId != null)
                newMemoEditText.addCheckBoxAndDotForExistMemo(
                    executeId.memoRowInfo, executeId.executionType, memoContents
                )

            formerMemoEditTextForExistMemo = newMemoEditText

            memoContents
        }
    }
}



private fun deleteMemoRow(
    executeId: DeleteMemoRow,
    memoContents: MemoContents
): MemoContents {
    Log.d("場所:deleteMemoRow", "deleteMemoRowに入った")

    val targetMemoEditText = executeId.memoEditText
    val indexOfTargetMemoRowInfo =
        memoContents.indexOfFirst { it.memoEditTextId.value == targetMemoEditText.id }
    val maxIndexOfList = memoContents.size - 1
    val formerMemoEditTextId = memoContents[indexOfTargetMemoRowInfo - 1].memoEditTextId.value
    val formerMemoEditText = editFragment.requireActivity().findViewById<MemoEditText>(formerMemoEditTextId)

    //FocusChangedListenerで処理をさせない為。プロパティの種類は何でも良い
    targetMemoEditText.isClickable = false

    //targetMemoEditTextの下に他のViewがある場合の制約のセット。無い場合はそのままViewを削除する。
    if (indexOfTargetMemoRowInfo < maxIndexOfList) {
        Log.d("場所:deleteMemoRow", "下に他のViewがある場合")
        val nextMemoEditTextId = memoContents[indexOfTargetMemoRowInfo + 1].memoEditTextId

        memoContainer.setConstraintForDeleteMemoRow(
            targetMemoEditText, MemoEditTextId(formerMemoEditTextId), nextMemoEditTextId
        )
    }

    memoContainer.removeMemoRowFromLayout(targetMemoEditText, formerMemoEditText)

    return memoContents.filter { it.memoEditTextId.value != targetMemoEditText.id }
}

private fun updateCheckBoxStateOfMemoContents(
    executeId: ChangeCheckBoxState,
    memoContents: MemoContents
): MemoContents = memoContents.map { memoRowInfo ->
    if (memoRowInfo.memoEditTextId.value == executeId.memoEditText.id)
        memoRowInfo.copy(checkBoxState = CheckBoxState(!memoRowInfo.checkBoxState.value))
    else memoRowInfo
}


private fun MemoEditText.changeTextColorByCheckBoxState(
    checkBoxState: Boolean,
    executionType: TypeOfMemoExecution
) {
    when {
        executionType is DisplayExistMemo && checkBoxState->
            this.setTextColor(resources.getColor(R.color.colorGray, displayFragment.activity?.theme))
        executionType is DisplayExistMemo && !checkBoxState->
            this.setTextColor(resources.getColor(R.color.colorBlack, displayFragment.activity?.theme))
        executionType !is DisplayExistMemo && checkBoxState->
            this.setTextColor(resources.getColor(R.color.colorGray, editFragment.activity?.theme))
        executionType !is DisplayExistMemo && !checkBoxState->
            this.setTextColor(resources.getColor(R.color.colorBlack, editFragment.activity?.theme))
    }
}

private fun CheckBox.setCheckedChangeAction(executeId: AddCheckBox, viewModel: ViewModel) {
    this@setCheckedChangeAction.setOnCheckedChangeListener { buttonView, isChecked ->
        val memoEditText = executeId.memoEditText
        Log.d("場所:setOnCheckedChangeListener", "targetMemoEditTextId=${memoEditText.id} targetCheckBoxId=${executeId.checkBoxId}")

        when (isChecked){
            true -> {
                Log.d("場所:setOnCheckedChangeListener", "isChecked=true")
                memoEditText.changeTextColorByCheckBoxState(true, executeId.executionType)
                viewModel.viewModelScope.launch {
                    executeActor.send(ChangeCheckBoxState(memoEditText, executeId.executionType))
                }
            }
            false -> {
                Log.d("場所:setOnCheckedChangeListener", "isChecked=false")
                memoEditText.changeTextColorByCheckBoxState(false, executeId.executionType)
                viewModel.viewModelScope.launch {
                    executeActor.send(ChangeCheckBoxState(memoEditText, executeId.executionType))
                }
            }
        }
    }
}

private fun createNewCheckBoxView(executeId: AddCheckBox): CheckBox {
    val context = when (executeId.executionType) {
        DisplayExistMemo -> displayFragment.context
        else -> editFragment.context
    }

    return CheckBox(context).apply {
        layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        ViewGroup.MarginLayoutParams(0, 0)
        textSize = 0f
        setPadding(4)
        id = when (executeId.executionType) {
            is CreateNewMemo -> View.generateViewId()
            is EditExistMemo , is DisplayExistMemo ->
                executeId.checkBoxId ?: throw(NullPointerException("CheckBoxId mast not be null"))
        }

        //既存メモの編集の場合、setCheckedChangeActionの前にCheckBoxViewを変更しておく
        if (executeId.checkBoxState) {
            isChecked = true
            executeId.memoEditText.changeTextColorByCheckBoxState(true, executeId.executionType)
        }

        when (executeId.executionType) {
            is DisplayExistMemo -> setCheckedChangeAction(executeId, searchViewModel)
            else -> setCheckedChangeAction(executeId, editViewModel)
        }
    }
}

private fun addCheckBox(
    executeId: AddCheckBox,
    memoContents: MemoContents
): MemoContents {
    Log.d("場所:addCheckBox", "checkBox追加処理に入った")

    val newCheckBox = createNewCheckBoxView(executeId)
    val memoEditText = executeId.memoEditText

    return when (executeId.executionType) {
        is CreateNewMemo -> {
            memoContainer.setConstraintForBulletsView(memoEditText, newCheckBox, 80)

            memoContents.flatMap { memoRowInfo ->
                if (memoRowInfo.memoEditTextId.value == memoEditText.id)
                    listOf(memoRowInfo.copy(
                        checkBoxId = CheckBoxId(newCheckBox.id), checkBoxState = CheckBoxState(false)
                    ))
                else listOf(memoRowInfo)
            }
        }
        else -> {
            memoContainer.setConstraintForBulletsView(memoEditText, newCheckBox, 80)

            memoContents
        }
    }
}

private fun deleteCheckBox(
    executeId: DeleteCheckBox,
    memoContents: MemoContents
): MemoContents {
    Log.d("場所:deleteCheckBox", "checkBox削除処理に入った")
    val memoEditText = executeId.memoEditText
    val targetMemoRowInfo = memoContents.first { it.memoEditTextId.value == memoEditText.id }

    memoContainer.apply {
        setConstraintForDeleteBulletsView(memoEditText)
        removeBulletsViewFromLayout(editFragment, memoEditText, targetMemoRowInfo.checkBoxId)
    }

    return memoContents.flatMap { memoRowInfo ->
        if (memoRowInfo.memoEditTextId.value == executeId.memoEditText.id)
            listOf(targetMemoRowInfo.copy(
                checkBoxId = CheckBoxId(null), checkBoxState = CheckBoxState(false)
            ))
        else listOf(memoRowInfo)
    }
}

private fun createDotTextView(executeId: AddDot): TextView {
    val context = when (executeId.executionType) {
        DisplayExistMemo -> displayFragment.context
        else -> editFragment.context
    }

    return TextView(context).apply {
        layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        setPadding(4)
        setBackgroundResource(R.color.colorTransparent)
        text = "・"
        id = when (executeId.executionType) {
            is CreateNewMemo -> {
                View.generateViewId()
            }
            is EditExistMemo, is DisplayExistMemo -> {
                executeId.dotId ?: throw(NullPointerException("DotId mast not be null"))
            }
        }
    }
}

private fun addDot(executeId: AddDot, memoContents: MemoContents): MemoContents {
    Log.d("場所:addDot", "dot追加処理に入った")

    val newDot = createDotTextView(executeId)
    val memoEditText = executeId.memoEditText

    return when (executeId.executionType) {
        is CreateNewMemo -> {
            memoContainer.setConstraintForBulletsView(memoEditText, newDot, 80, 40)

            memoContents.flatMap { memoRowInfo ->
                if (memoRowInfo.memoEditTextId.value == executeId.memoEditText.id)
                    listOf(memoRowInfo.copy(dotId = DotId(newDot.id)))
                else listOf(memoRowInfo)
            }
        }
        else -> {
            memoContainer.setConstraintForBulletsView(executeId.memoEditText, newDot, 80, 40)

            memoContents
        }
    }
}

private fun deleteDot(executeId: DeleteDot, memoContents: MemoContents): MemoContents {
    Log.d("場所:deleteDot", "dotの削除処理に入った")
    val memoEditText = executeId.memoEditText
    val targetMemoRowInfo = memoContents.first { it.memoEditTextId.value == memoEditText.id }

    memoContainer.apply {
        setConstraintForDeleteBulletsView(memoEditText)
        removeBulletsViewFromLayout(editFragment, memoEditText, targetMemoRowInfo.dotId)
    }

    return memoContents.flatMap { memoRowInfo ->
        if (memoRowInfo.memoEditTextId.value == executeId.memoEditText.id)
            listOf(targetMemoRowInfo.copy(dotId = DotId(null)))
        else listOf(memoRowInfo)
    }
}

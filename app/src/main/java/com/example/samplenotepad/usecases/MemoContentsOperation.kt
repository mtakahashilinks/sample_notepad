package com.example.samplenotepad.usecases

import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
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
import arrow.core.*
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
private lateinit var formerMemoEditTextForExistMemo: MemoEditText //databaseから読みだしたメモの編集や表示の際のみ使う

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
                getMemoContentsExecuteActor().send(SetMemoContents(listOf<MemoRowInfo>().k()))

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
    val memoRowInfo =
        memoContents[memoContents.indexOfFirst { it.memoEditTextId.value == this@checkBoxOperation.id }]
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
    val memoRowInfo =
        memoContents[memoContents.indexOfFirst { it.memoEditTextId.value == this@dotOperation.id }]
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
        var memoContents = listOf<MemoRowInfo>().k()

        for (msg in channel) {
            Log.d("場所:memoContentsOperationActor", "memoContentsOperationActorに入った executeId=$msg")
            Log.d("場所:memoContentsOperationActor", "変更前:size=${memoContents.size} memoContents=$memoContents")

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

            Log.d("場所:memoContentsOperationActor", "変更後:size=${memoContents.size} memoContents=$memoContents")
            Log.d("場所:memoContentsOperationActor", "executeMemoOperationが終わった executeId=$msg")
        }
    }


private fun updateTextOfMemoRowInfo(
    executeId: UpdateTextOfMemoRowInfo,
    memoContents: MemoContents
): MemoContents {
    return memoContents.flatMap { memoRowInfo ->
        val indexOfTargetMemoRow =
            memoContents.indexOfFirst { it.memoEditTextId.value == executeId.memoEditText.id }

        if (memoRowInfo.memoEditTextId.value == executeId.memoEditText.id)
            listOf(memoContents[indexOfTargetMemoRow].copy(
                memoText = Text(executeId.memoEditText.text.toString()))
            ).k()
        else listOf(memoRowInfo).k()
    }
}


private fun MemoEditText.setEnterKeyAction() {
    this.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            Log.d("場所:beforeTextChanged", "s=$s start=$start  count=$count after=$after")
            if (this@setEnterKeyAction.selectionEnd != 0)
                editViewModel.updateIfAtFirstInText(false)
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            Log.d("場所:onTextChanged", "s=$s start=$start before=$before count=$count")
        }

        override fun afterTextChanged(s: Editable?) {
            Log.d("場所:afterTextChanged", "s=$s")
            when {
                s !== null && """\n""".toRegex().containsMatchIn(s.toString()) -> {
                    Log.d("場所:afterTextChanged", "改行処理に入った")
                    val textBringToNextRow = s.toString().substringAfter("\n")

                    this@setEnterKeyAction.setText(
                        s.toString().replace("\n" + textBringToNextRow, ""),
                        TextView.BufferType.NORMAL
                    )

                    editViewModel.viewModelScope.launch {
                        executeActor.send(CreateNextMemoEditText(Text(textBringToNextRow), CreateNewMemo))
                    }
                }
                else -> return
            }
        }
    } )
}

//private fun MemoEditText.setBackSpaceKeyAction(executeId: TypeForExecuteMemoContents) {
//    this.setOnKeyListener { v, code, event ->
//        if (event.action == KeyEvent.ACTION_UP && executeId is CreateNextMemoEditText && v is MemoEditText) {
//            when {
//                code == KeyEvent.KEYCODE_DEL && editViewModel.getIfAtFirstInText() -> {
//                    Log.d("場所:setOnKeyListener", "Delキーイベントに入った")
//
//                    val memoContents = editViewModel.getMemoContents()
//                    val memoRowInfo =
//                        memoContents[memoContents.indexOfFirst { it.memoEditTextId.value == v.id }]
//
//                    Log.d("場所:setOnKeyListener", "削除するMemoEditTextのId=${v.id}")
//                    Log.d("場所:setOnKeyListener", "selectionEnd=${v.selectionEnd}")
//                    Log.d("場所:setOnKeyListener", "size=${memoContents.size} memoContents=${memoContents}")
//
//                    editViewModel.viewModelScope.launch {
//                        when {
//                            memoRowInfo.checkBoxId.value != null -> executeActor.send(DeleteCheckBox(v))
//                            memoRowInfo.dotId.value != null -> executeActor.send(DeleteDot(v))
//                        }
//
//                        executeActor.send(DeleteMemoRow(v))
//                    }
//
//                    Log.d("場所:setOnKeyListener", "ifAtFirstInText=${editViewModel.getIfAtFirstInText()}")
//                }
//                //このタイミングでフラグをtrueに変更しないと、カーソルが文頭に移動した瞬間に削除処理に入ってしまう
//                // (カーソルが文頭に移動た後にDELキーを押した時点で削除処理に入ってほしい)
//                v.selectionEnd == 0 -> {
//                    editViewModel.updateIfAtFirstInText(true)
//
//                    Log.d("場所:setOnKeyListener#flag=false", "ifAtFirstInText=${editViewModel.getIfAtFirstInText()}")
//                }
//            }
//        }
//        false
//    }
//}

private fun MemoEditText.setFocusChangeAction() {
    //フォーカスが他に移るタイミングでMemoRowInfoのTextを更新する
    setOnFocusChangeListener { v, hasFocus ->
        when {
            //v.isClickableはMemoRowのdelete処理の時に呼ばれない為
            v is MemoEditText && v.isClickable && !hasFocus -> {
                Log.d("場所:setOnFocusChangeListener", "FocusChange(Lost)が呼ばれた memoEditTextId=${v.id}")

                editViewModel.viewModelScope.launch { executeActor.send(UpdateTextOfMemoRowInfo(v)) }

                //なぜかEditExistMemoでinitした時にFocusが外れてしまうので取得しなおす
             //   if (memoContainer.focusedChild == null) v.setFocus()
            }
            hasFocus -> {
                Log.d("場所:setOnFocusChangeListener", "FocusChange(Get)が呼ばれた memoEditTextId=${v.id}")

                editViewModel.updateIfAtFirstInText(true)
                Log.d("場所:setOnFocusChangeListener", "ifAtFirstInText=${editViewModel.getIfAtFirstInText()}")
            }
        }
    }
}

private fun MemoEditText.setTouchAction() {
    setOnTouchListener { v, event ->
        Log.d("場所:setOnTouchListener", "setOnTouchListenerが呼ばれた memoEditTextId=${v.id}")

        editViewModel.updateIfAtFirstInText(true)
        Log.d("場所:setOnTouchListener", "ifAtFirstInText=${editViewModel.getIfAtFirstInText()}")

        false
    }
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
    memoEditTextId: Int?,
    text : Text,
    executeType: TypeOfMemoExecution
): MemoEditText {
    fun MemoEditText.setActionsAndText() {
        setFocusChangeAction()
        setTouchAction()
        //setBackSpaceKeyAction(executeId)
        setEnterKeyAction()
    }

    return when (executeType) {
        is CreateNewMemo -> {
            MemoEditText(editFragment.context, editViewModel, memoContainer, executeActor).apply {
                id = View.generateViewId()
                setCommonLayoutParams()
                setActionsAndText()
            }
        }
        is EditExistMemo -> {
            MemoEditText(editFragment.context, editViewModel, memoContainer, executeActor).apply {
                setCommonLayoutParams()
                id = memoEditTextId ?: throw(NullPointerException("memoEditTextId mast be not null"))
                setText(text.value)
                setActionsAndText()
            }
        }
        is DisplayExistMemo -> {
            MemoEditText(displayFragment.context, editViewModel, memoContainer, executeActor).apply {
                setCommonLayoutParams()
                id = memoEditTextId ?: throw(NullPointerException("memoEditTextId mast be not null"))
                setText(text.value)
                isFocusable = false
                isFocusableInTouchMode = false
            }
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

            newMemoEditText.setFocusAndTextAndCursorPosition(editViewModel, executeId.text)

            listOf(MemoRowInfo(MemoEditTextId(newMemoEditText.id), executeId.text)).k()
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
            val indexOfTargetMemoRow =
                memoContents.indexOfFirst { it.memoEditTextId.value == targetMemoEditTextId }
            val maxIndexOfList = memoContents.size - 1

            when (indexOfTargetMemoRow == maxIndexOfList) {
                 true-> {
                    Log.d("場所:createNextMemoEditText", "下に他のViewがない場合のLayoutの制約設定とMemoContentsのUpdate")
                    memoContainer.setConstraintForNextMemoEditTextWithNoBelow(
                        newMemoEditText,
                        MemoEditTextId(targetMemoEditTextId),
                        editViewModel,
                        executeId.text
                    )

                    memoContents.combineK(listOf(MemoRowInfo(MemoEditTextId(newMemoEditText.id))).k())
                }
                false -> {
                    Log.d("場所:createNextMemoEditText", "下に他のViewがある場合のLayoutの制約とMemoContentsのUpdate")
                    val nextMemoEditTextId = memoContents[indexOfTargetMemoRow + 1].memoEditTextId
                    val prefixList = memoContents.take(indexOfTargetMemoRow + 1).k()
                    val suffixList = memoContents.drop(indexOfTargetMemoRow + 1).k()

                    memoContainer.setConstraintForNextMemoEditTextWithBelow(
                        newMemoEditText,
                        MemoEditTextId(targetMemoEditTextId),
                        nextMemoEditTextId,
                        editViewModel,
                        executeId.text
                    )

                    prefixList
                        .combineK(listOf(MemoRowInfo(MemoEditTextId(newMemoEditText.id))).k())
                        .combineK(suffixList)
                }
            }
        }
        else -> {
            //既存のメモの表示(DisplayExistMemo)か編集(EditExistMemo)の場合
            memoContainer.setConstraintForNextMemoEditTextWithNoBelow(
                newMemoEditText,
                MemoEditTextId(formerMemoEditTextForExistMemo.id),
                editViewModel,
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
    val indexOfTargetMemoRow =
        memoContents.indexOfFirst { it.memoEditTextId.value == targetMemoEditText.id }
    val maxIndexOfList = memoContents.size - 1
    val formerMemoEditTextId = memoContents[indexOfTargetMemoRow - 1].memoEditTextId.value
    val formerMemoEditText = editFragment.requireActivity().findViewById<MemoEditText>(formerMemoEditTextId)

    //FocusChangedListenerで処理をさせない為。プロパティの種類は何でも良い
    targetMemoEditText.isClickable = false

    //targetMemoEditTextの下に他のViewがある場合の制約のセット。無い場合はそのままViewを削除する。
    if (indexOfTargetMemoRow < maxIndexOfList) {
        Log.d("場所:deleteMemoRow", "下に他のViewがある場合")
        val nextMemoEditTextId = memoContents[indexOfTargetMemoRow + 1].memoEditTextId

        memoContainer.setConstraintForDeleteMemoRow(
            targetMemoEditText, MemoEditTextId(formerMemoEditTextId), nextMemoEditTextId
        )
    }

    memoContainer.removeMemoRowFromLayout(targetMemoEditText, formerMemoEditText, editViewModel)

    return memoContents.filter { it.memoEditTextId.value != targetMemoEditText.id }.k()
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
                val indexOfTargetMemoRow =
                    memoContents.indexOfFirst { it.memoEditTextId.value == memoEditText.id }

                if (memoRowInfo.memoEditTextId.value == memoEditText.id)
                    listOf(memoContents[indexOfTargetMemoRow].copy(
                        checkBoxId = CheckBoxId(newCheckBox.id),
                        checkBoxState = CheckBoxState(false)
                    )).k()
                else listOf(memoRowInfo).k()
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
    val indexOfTargetMemoRow = memoContents.indexOfFirst { it.memoEditTextId.value == memoEditText.id }
    val checkBoxId = memoContents[indexOfTargetMemoRow].checkBoxId

    memoContainer.apply {
        setConstraintForDeleteBulletsView(memoEditText)
        removeBulletsViewFromLayout(editFragment, memoEditText, checkBoxId)
    }

    return memoContents.flatMap { memoRowInfo ->
        if (memoRowInfo.memoEditTextId.value == executeId.memoEditText.id)
            listOf(memoContents[indexOfTargetMemoRow].copy(
                checkBoxId = CheckBoxId(null),
                checkBoxState = CheckBoxState(false)
            )).k()
        else listOf(memoRowInfo).k()
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

            val indexOfTargetMemoRow =
                memoContents.indexOfFirst { it.memoEditTextId.value == memoEditText.id }

            memoContents.flatMap { memoRowInfo ->
                if (memoRowInfo.memoEditTextId.value == executeId.memoEditText.id)
                    listOf(memoContents[indexOfTargetMemoRow].copy(dotId = DotId(newDot.id))).k()
                else listOf(memoRowInfo).k()
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
    val indexOfTargetMemoRow = memoContents.indexOfFirst { it.memoEditTextId.value == memoEditText.id }
    val dotId = memoContents[indexOfTargetMemoRow].dotId

    memoContainer.apply {
        setConstraintForDeleteBulletsView(memoEditText)
        removeBulletsViewFromLayout(editFragment, memoEditText, dotId)
    }

    return memoContents.flatMap { memoRowInfo ->
        if (memoRowInfo.memoEditTextId.value == executeId.memoEditText.id)
            listOf(memoContents[indexOfTargetMemoRow].copy(dotId = DotId(null))).k()
        else listOf(memoRowInfo).k()
    }
}

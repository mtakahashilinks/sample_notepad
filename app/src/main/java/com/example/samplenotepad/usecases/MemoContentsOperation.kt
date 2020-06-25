package com.example.samplenotepad.usecases

import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.*
import com.example.samplenotepad.*
import com.example.samplenotepad.data.saveMemoInfo
import com.example.samplenotepad.entities.*
import com.example.samplenotepad.viewModels.MemoEditViewModel
import com.example.samplenotepad.viewModels.SearchViewModel
import com.example.samplenotepad.views.main.*
import com.example.samplenotepad.views.main.setConstraintForFirstMemoRow
import com.example.samplenotepad.views.main.setConstraintForNextMemoRowWithNoBelow
import com.example.samplenotepad.views.main.setFocusAndTextAndCursorPosition
import com.example.samplenotepad.views.search.DisplayMemoFragment
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor


internal lateinit var firstMemoRow: MemoRow
private lateinit var editFragment: MemoEditFragment
private lateinit var displayFragment: DisplayMemoFragment
private lateinit var editViewModel: MemoEditViewModel
private lateinit var searchViewModel: SearchViewModel
private lateinit var memoContainer: ConstraintLayout
private lateinit var executeActor: SendChannel<TypeForExecuteMemoContents>
private lateinit var formerMemoRowForExistMemo: MemoRow //databaseから読みだしたメモの編集や表示の際のみ使う


@ObsoleteCoroutinesApi
internal fun initMemoContentsOperation(
    fragment: Fragment,
    viewModel: ViewModel,
    container: ConstraintLayout,
    executionType: WhichMemoExecution
) = runBlocking {
    when (executionType){
        is CreateNewMemo -> {
            editFragment = fragment as MemoEditFragment
            editViewModel = viewModel as MemoEditViewModel
            executeActor = viewModel.viewModelScope.executeMemoOperation()
            memoContainer = container

            editViewModel.apply {
                updateMemoContents { listOf<MemoRowInfo>().k() }

                executeActor.send(CreateFirstMemoRow(Text(""), CreateNewMemo))

                updateMemoContentsAtSavePoint()
            }
        }
        is EditExistMemo -> {
            editFragment = fragment as MemoEditFragment
            editViewModel = viewModel as MemoEditViewModel
            executeActor = viewModel.viewModelScope.executeMemoOperation()
            memoContainer = container
            Log.d("場所:initMemoContentsOperation", "memoId=${editViewModel.getMemoInfo()?.rowid} memoContents=${editViewModel.getMemoContents()}")

            createMemoRowsForExistMemo(executionType, editViewModel.getMemoContents())
        }
        is DisplayExistMemo -> {
            displayFragment = fragment as DisplayMemoFragment
            searchViewModel = viewModel as SearchViewModel
            executeActor = viewModel.viewModelScope.executeMemoOperation()
            memoContainer = container

            createMemoRowsForExistMemo(executionType, searchViewModel.getMemoContents())
        }
    }
}

internal fun closeMemoContentsOperation() = executeActor.close()

//ボタンがクリックされた時のcheckBox処理の入り口
internal fun MemoRow.checkBoxOperation() = runBlocking {
    val memoContents = editViewModel.getMemoContents()
    val memoRowInfo =
        memoContents[memoContents.indexOfFirst { it.memoRowId.value == this@checkBoxOperation.id }]
    val checkBoxId = memoRowInfo.checkBoxId.value

    when {
        memoRowInfo.dotId.value is Some<Int> -> {
            executeActor.send(DeleteDot(this@checkBoxOperation))
            executeActor.send(AddCheckBox(this@checkBoxOperation, CreateNewMemo))
        }
        checkBoxId is None -> executeActor.send(AddCheckBox(this@checkBoxOperation, CreateNewMemo))
        checkBoxId is Some<Int> -> executeActor.send(DeleteCheckBox(this@checkBoxOperation))
    }
}

//ボタンがクリックされた時のdot処理の入り口
internal fun MemoRow.dotOperation() = runBlocking {
    val mList = editViewModel.getMemoContents()
    val memoRowInfo = mList[mList.indexOfFirst { it.memoRowId.value == this@dotOperation.id }]
    val dotId = memoRowInfo.dotId.value

    when {
        memoRowInfo.checkBoxId.value is Some<Int> -> {
            executeActor.send(DeleteCheckBox(this@dotOperation))
            executeActor.send(AddDot(this@dotOperation, CreateNewMemo))
        }
        dotId is None -> executeActor.send(AddDot(this@dotOperation, CreateNewMemo))
        dotId is Some<Int> -> executeActor.send(DeleteDot(this@dotOperation))
    }
}

internal fun clearAll() = runBlocking {
    Log.d("場所:clearAll", "ClearAll処理に入った")

    memoContainer.removeAllViews()

    editViewModel.viewModelScope.launch {
        executeActor.send(CreateFirstMemoRow(Text(""), CreateNewMemo))
    }.join()

    editViewModel.updateMemoContentsAtSavePoint()
}

internal fun saveMemo(executionType: WhichMemoExecution) = runBlocking {
    //フォーカスを外しすことでupdateTextOfMemoRowInfoが呼ばれてTextプロパティが更新される
    memoContainer.clearFocus()

    executeActor.send(SaveMemoInfo(executionType))
}

private fun saveOperation(executeId: SaveMemoInfo) = runBlocking {
    Log.d("saveOperation", "save処理に入った")

    saveMemoInfo(
        executeId.executionType,
        editViewModel,
        editViewModel.getMemoInfo(),
        editViewModel.getMemoContents()
    )
}

private fun createMemoRowsForExistMemo(
    executionType: WhichMemoExecution,
    memoContents: MemoContents
) = runBlocking {
    suspend fun List<MemoRowInfo>.createFirstRow(): List<MemoRowInfo> {
        val targetMemoRowInfo = this@createFirstRow[0]

        executeActor.send(CreateFirstMemoRow(targetMemoRowInfo.text, executionType, targetMemoRowInfo))

        return this.drop(1)
    }

    suspend fun List<MemoRowInfo>.createNextRow() {
        this.onEach { memoRowInfo ->
            executeActor.send(CreateNextMemoRow(memoRowInfo.text, executionType, memoRowInfo))
        }
    }

    Log.d("場所:createMemoRowsForExistMemo", "memoContents=${memoContents.toList()}")

    //主要な処理
    when (executionType) {
        is DisplayExistMemo -> {
            memoContents.toList().createFirstRow().createNextRow()

            searchViewModel.updateMemoContentsAtSavePoint()
        }
        else -> {
            memoContents.toList().createFirstRow().createNextRow()

            editViewModel.updateMemoContentsAtSavePoint()
        }
    }
}

private fun MemoRow.addCheckBoxAndDot(
    memoRowInfo: MemoRowInfo,
    executionType: WhichMemoExecution
) {
    if (memoRowInfo.checkBoxId.value is Some) {
        val checkBoxId = memoRowInfo.checkBoxId.value.getOrElse {
            throw(NullPointerException("CheckBoxId must not be null"))
        }
        val checkBoxState = memoRowInfo.checkBoxState.value

        addCheckBox(AddCheckBox(this, executionType, checkBoxId, checkBoxState))
    }

    if (memoRowInfo.dotId.value is Some) {
        val dotId = memoRowInfo.dotId.value.getOrElse {
            throw(NullPointerException("DotId must not be null"))
        }

        addDot(AddDot(this, executionType, dotId))
    }
}


//近い将来、代替えのAPIに切り替わるらしいので要注意
@ObsoleteCoroutinesApi
private fun CoroutineScope.executeMemoOperation() = actor<TypeForExecuteMemoContents> {
        for (msg in channel) {
            Log.d("場所:executeMemoOperation", "executeMemoOperationに入った executeId=$msg")
            when (msg) {
                is UpdateTextOfMemoRowInfo -> updateTextOfMemoRowInfo(msg)
                is CreateFirstMemoRow -> createFirstMemoRow(msg)
                is CreateNextMemoRow -> createNextMemoRow(msg)
                is DeleteMemoRow -> deleteMemoRow(msg)
                is AddCheckBox -> addCheckBox(msg)
                is DeleteCheckBox -> deleteCheckBox(msg)
                is ChangeCheckBoxState -> switchByExecutionTypeForUpdateCheckBoxState(msg)
                is AddDot -> addDot(msg)
                is DeleteDot -> deleteDot(msg)
                is SaveMemoInfo -> saveOperation(msg)
            }

            Log.d("場所:executeMemoOperation", "executeMemoOperationが終わった executeId=$msg")
        }
    }


private fun updateTextOfMemoRowInfo(executeId: UpdateTextOfMemoRowInfo) {
    editViewModel.updateMemoContents { memoContents ->
        val indexOfMemoRow = memoContents.indexOfFirst { it.memoRowId.value == executeId.memoRow.id }

        Log.d("場所:updateTextOfMemoContents",
            "変更前:size=${memoContents.size} MemoContents=${memoContents}")

        memoContents.flatMap { mMemoRowInfo ->
            if (mMemoRowInfo.memoRowId.value == executeId.memoRow.id)
                listOf(memoContents[indexOfMemoRow].copy(text = Text(executeId.memoRow.text.toString()))).k()
            else listOf(mMemoRowInfo).k()
        }
    }
    Log.d("場所:updateTextOfMemoContents",
        "変更後:size=${editViewModel.getMemoContents().size} memoContents=${editViewModel.getMemoContents()}")
}


private fun MemoRow.setEnterKeyAction() {
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
                        executeActor.send(CreateNextMemoRow(Text(textBringToNextRow), CreateNewMemo))
                    }
                }
                else -> return
            }
        }
    } )
}

private fun MemoRow.setBackSpaceKeyAction(executeId: TypeForExecuteMemoContents) {
    this.setOnKeyListener { v, code, event ->
        if (event.action == KeyEvent.ACTION_UP && executeId is CreateNextMemoRow && v is MemoRow) {
            when {
                code == KeyEvent.KEYCODE_DEL && editViewModel.getIfAtFirstInText() -> {
                    Log.d("場所:setOnKeyListener", "Delキーイベントに入った")

                    val memoContents = editViewModel.getMemoContents()
                    val memoRowInfo =
                        memoContents[memoContents.indexOfFirst { it.memoRowId.value == v.id }]

                    Log.d("場所:setOnKeyListener", "削除するMemoRowのId=${v.id}")
                    Log.d("場所:setOnKeyListener", "selectionEnd=${v.selectionEnd}")
                    Log.d("場所:setOnKeyListener", "size=${memoContents.size} memoContents=${memoContents}")

                    editViewModel.viewModelScope.launch {
                        when {
                            memoRowInfo.checkBoxId.value is Some -> executeActor.send(DeleteCheckBox(v))
                            memoRowInfo.dotId.value is Some -> executeActor.send(DeleteDot(v))
                        }

                        executeActor.send(DeleteMemoRow(v))
                    }

                    Log.d("場所:setOnKeyListener", "ifAtFirstInText=${editViewModel.getIfAtFirstInText()}")
                }
                //このタイミングでフラグをtrueに変更しないと、カーソルが文頭に移動した瞬間に削除処理に入ってしまう
                // (カーソルが文頭に移動た後にDELキーを押した時点で削除処理に入ってほしい)
                v.selectionEnd == 0 -> {
                    editViewModel.updateIfAtFirstInText(true)

                    Log.d("場所:setOnKeyListener#flag=false", "ifAtFirstInText=${editViewModel.getIfAtFirstInText()}")
                }
            }
        }
        false
    }
}

private fun MemoRow.setFocusChangeAction() {
    //フォーカスが他に移るタイミングでMemoRowInfoのTextを更新する
    setOnFocusChangeListener { v, hasFocus ->
        when {
            //v.isClickableはMemoRowのdelete処理の時に呼ばれない為
            v is MemoRow && v.isClickable && !hasFocus -> {
                Log.d("場所:setOnFocusChangeListener", "FocusChange(Lost)が呼ばれた memoRowId=${v.id}")

                editViewModel.viewModelScope.launch { executeActor.send(UpdateTextOfMemoRowInfo(v)) }

                //なぜかEditExistMemoでinitした時にFocusが外れてしまうので取得しなおす
             //   if (memoContainer.focusedChild == null) v.setFocus()
            }
            hasFocus -> {
                Log.d("場所:setOnFocusChangeListener", "FocusChange(Get)が呼ばれた memoRowId=${v.id}")

                editViewModel.updateIfAtFirstInText(true)
                Log.d("場所:setOnFocusChangeListener", "ifAtFirstInText=${editViewModel.getIfAtFirstInText()}")
            }
        }
    }
}

private fun MemoRow.setTouchAction() {
    setOnTouchListener { v, event ->
        Log.d("場所:setOnTouchListener", "setOnTouchListenerが呼ばれた memoRowId=${v.id}")

        editViewModel.updateIfAtFirstInText(true)
        Log.d("場所:setOnTouchListener", "ifAtFirstInText=${editViewModel.getIfAtFirstInText()}")

        false
    }
}

private fun createNewMemoRowView(
    executeId: TypeForExecuteMemoContents,
    memoRowId: Int?,
    memoRowText : Text,
    whichExecute: WhichMemoExecution
): MemoRow {
    fun EditText.setActionsAndText() {
        setFocusChangeAction()
        setTouchAction()
        setBackSpaceKeyAction(executeId)
        setEnterKeyAction()
    }

    fun setLayoutParamForEditText() =
        ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT
        )

    return when (whichExecute) {
        is CreateNewMemo -> {
            EditText(editFragment.context, null, 0, R.style.MemoRowViewStyle).apply {
                layoutParams = setLayoutParamForEditText()
                id = View.generateViewId()
                setActionsAndText()
            }
        }
        is EditExistMemo -> {
            EditText(editFragment.context, null, 0, R.style.MemoRowViewStyle).apply {
                layoutParams = setLayoutParamForEditText()
                id = memoRowId ?: throw(NullPointerException("memoRowId mast not be null"))
                setText(memoRowText.value)
                setActionsAndText()
            }
        }
        is DisplayExistMemo -> {
            EditText(displayFragment.context, null, 0, R.style.MemoRowViewStyle).apply {
                layoutParams = setLayoutParamForEditText()
                id = memoRowId ?: throw(NullPointerException("memoRowId mast not be null"))
                setText(memoRowText.value)
                isFocusableInTouchMode = false
                isFocusable = false
            }
        }
    }
}

//MemoContentsの最初の行をセットする
private fun createFirstMemoRow(executeId: CreateFirstMemoRow) {
    Log.d("場所:createFirstMemoRow", "createFirstMemoRowに入った")

    val text = executeId.text
    val newMemoRow = createNewMemoRowView(
        executeId, executeId.memoRowInfo?.memoRowId?.value, text, executeId.executionType
    )

    firstMemoRow = newMemoRow

    when (executeId.executionType) {
        is CreateNewMemo -> {
            editViewModel.updateMemoContents { memoContents ->
                editViewModel.viewModelScope.launch(Dispatchers.Main) {
                    memoContainer.setConstraintForFirstMemoRow(newMemoRow)
                }

                Log.d("場所:createFirstMemoRow", "newMemoRowId=${newMemoRow.id}")
                Log.d("場所:createFirstMemoRow", "変更前:size=${memoContents.size} memoContents=${memoContents}")

                listOf(MemoRowInfo(MemoRowId(newMemoRow.id), text)).k()
            }
            Log.d("場所:createFirstMemoRow",
                "変更後:size=${editViewModel.getMemoContents().size} memoContents=${editViewModel.getMemoContents()}")

            newMemoRow.setFocusAndTextAndCursorPosition(editViewModel, executeId.text)
        }
        else -> {
            memoContainer.setConstraintForFirstMemoRow(newMemoRow)

            if (executeId.memoRowInfo != null) {
                newMemoRow.addCheckBoxAndDot(executeId.memoRowInfo, executeId.executionType)
            }

            formerMemoRowForExistMemo = newMemoRow
        }
    }
}

private fun createNextMemoRow(executeId: CreateNextMemoRow) {
    Log.d("場所:createNextMemoRow", "createNextMemoRowに入った")

    val newMemoRow = createNewMemoRowView(
        executeId, executeId.memoRowInfo?.memoRowId?.value, executeId.text, executeId.executionType
    )

    when (executeId.executionType) {
        is CreateNewMemo -> {
            editViewModel.updateMemoContents { memoContents ->
                val targetMemoRowId = memoContainer.findFocus().id
                val indexOfTargetMemoRow = memoContents.indexOfFirst { it.memoRowId.value == targetMemoRowId }
                val maxIndexOfList = memoContents.size - 1

                Log.d("場所:createNextMemoRow", "text=${executeId.text}")
                Log.d("場所:createNextMemoRow", "targetMemoRowId=$targetMemoRowId")
                Log.d("場所:createNextMemoRow", "newMemoRowId=${newMemoRow.id}")
                Log.d("場所:createNextMemoRow", "indexOfTargetMemoRow=$indexOfTargetMemoRow")
                Log.d("場所:createNextMemoRow", "maxIndexOfList=$maxIndexOfList")

                when {
                    maxIndexOfList == indexOfTargetMemoRow -> {
                        Log.d("場所:createNextMemoRow", "下に他のViewがない場合")
                        memoContainer.setConstraintForNextMemoRowWithNoBelow(
                            newMemoRow,
                            MemoRowId(targetMemoRowId),
                            editViewModel,
                            executeId.text
                        )
                    }
                    maxIndexOfList > indexOfTargetMemoRow -> {
                        Log.d("場所:createNextMemoRow", "下に他のViewがある場合")
                        val nextMemoRowId = memoContents[indexOfTargetMemoRow + 1].memoRowId

                        memoContainer.setConstraintForNextMemoRowWithBelow(
                            newMemoRow,
                            MemoRowId(targetMemoRowId),
                            nextMemoRowId,
                            editViewModel,
                            executeId.text
                        )
                    }
                }

                Log.d("場所:createNextMemoRow", "変更前:size=${memoContents.size} memoContents=$memoContents")

                when {
                    indexOfTargetMemoRow < maxIndexOfList -> {
                        Log.d("場所:createNextMemoRow", "indexがリストサイズより小さい場合")
                        val prefixList = memoContents.take(indexOfTargetMemoRow + 1).k()
                        val suffixList = memoContents.drop(indexOfTargetMemoRow + 1).k()

                        prefixList
                            .combineK(listOf(MemoRowInfo(MemoRowId(newMemoRow.id))).k())
                            .combineK(suffixList)
                    }
                    else -> {
                        Log.d("場所:createNextMemoRow", "indexがリストの最後尾の場合")
                        memoContents.combineK(listOf(MemoRowInfo(MemoRowId(newMemoRow.id))).k())
                    }
                }
            }

            Log.d("場所:createNextMemoRow",
                "変更後:size=${editViewModel.getMemoContents().size} MemoContents=${editViewModel.getMemoContents()}")
        }
        else -> {
            memoContainer.setConstraintForNextMemoRowWithNoBelow(
                newMemoRow, MemoRowId(formerMemoRowForExistMemo.id), editViewModel, executeId.text
            )

            if (executeId.memoRowInfo != null)
                newMemoRow.addCheckBoxAndDot(executeId.memoRowInfo, executeId.executionType)

            formerMemoRowForExistMemo = newMemoRow
        }
    }
}

private fun deleteMemoRow(executeId: DeleteMemoRow) {
    Log.d("場所:deleteMemoRow", "deleteMemoRowに入った")

    editViewModel.updateMemoContents { memoContents ->
        val targetMemoRow = executeId.memoRow
        val indexOfTargetMemoRow = memoContents.indexOfFirst { it.memoRowId.value == targetMemoRow.id }
        val maxIndexOfList = memoContents.size - 1
        val formerMemoRowId = memoContents[indexOfTargetMemoRow - 1].memoRowId.value
        val formerMemoRow = editFragment.requireActivity().findViewById<EditText>(formerMemoRowId)

        Log.d("場所:deleteMemoRow", "targetMemoRowId=${targetMemoRow.id}")
        Log.d("場所:deleteMemoRow", "indexOfTargetMemoRow=$indexOfTargetMemoRow")
        Log.d("場所:deleteMemoRow", "maxIndexOfList=$maxIndexOfList")
        Log.d("場所:deleteMemoRow", "変更前:size=${memoContents.size} memoContents=${memoContents}")

        //FocusChangedListenerで処理をさせない為。プロパティの種類は何でも良い
        targetMemoRow.isClickable = false

        //targetMemoRowの下に他のViewがある場合の制約のセット。無い場合はそのままViewを削除する。
        if (maxIndexOfList > indexOfTargetMemoRow) {
            Log.d("場所:deleteMemoRow", "下に他のViewがある場合")
            val nextMemoRowId = memoContents[indexOfTargetMemoRow + 1].memoRowId

            memoContainer.setConstraintForDeleteMemoRow(
                targetMemoRow, MemoRowId(formerMemoRowId), nextMemoRowId
            )
        }

        memoContainer.removeMemoRowFromLayout(targetMemoRow, formerMemoRow, editViewModel)

        memoContents.filter { it.memoRowId.value != targetMemoRow.id }.k()
    }
    Log.d("場所:deleteMemoRow",
        "変更後:size=${editViewModel.getMemoContents().size} MemoContents=${editViewModel.getMemoContents()}")
}

private fun MemoContents.updateCheckBoxStateInMemoContents(memoRowId: Int): MemoContents {
    val indexOfMemoRow = this.indexOfFirst { it.memoRowId.value == memoRowId}

    return this.flatMap {
        if (it.memoRowId.value == memoRowId)
            listOf(this[indexOfMemoRow].copy(checkBoxState = CheckBoxState(!it.checkBoxState.value))).k()
        else listOf(it).k()
    }
}
private fun MemoRow.changeTextColorByCheckBoxState(checkBoxState: Boolean, executionType: WhichMemoExecution) {
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

private fun switchByExecutionTypeForUpdateCheckBoxState(executeId: ChangeCheckBoxState) =
    when (executeId.executionType) {
        is DisplayExistMemo -> {
            searchViewModel.updateMemoContents { memoContents ->
                Log.d("場所:changeCheckBoxState", "変更前:size=${memoContents.size} memoContents=${memoContents}")
                memoContents.updateCheckBoxStateInMemoContents(executeId.memoRow.id)
            }
            Log.d("場所:changeCheckBoxState", "変更後:size=${searchViewModel.getMemoContents().size} memoContents=${searchViewModel.getMemoContents()}")
        }
        else -> {
            editViewModel.updateMemoContents { memoContents ->
                Log.d("場所:changeCheckBoxState", "変更前:size=${memoContents.size} memoContents=${memoContents}")
                memoContents.updateCheckBoxStateInMemoContents(executeId.memoRow.id)
            }
            Log.d("場所:changeCheckBoxState", "変更後:size=${editViewModel.getMemoContents().size} memoContents=${editViewModel.getMemoContents()}")
        }
    }

private fun CheckBox.setCheckedChangeAction(executeId: AddCheckBox) {
    this.setOnCheckedChangeListener { buttonView, isChecked ->
        val memoRow = executeId.memoRow
        Log.d("場所:setOnCheckedChangeListener", "targetMemoRowId=${memoRow.id} targetCheckBoxId=${executeId.checkBoxId}")

        editViewModel.viewModelScope.launch {
            when (isChecked){
                true -> {
                    memoRow.changeTextColorByCheckBoxState(true, executeId.executionType)
                    executeActor.send(ChangeCheckBoxState(memoRow, executeId.executionType))
                }
                false -> {
                    memoRow.changeTextColorByCheckBoxState(false, executeId.executionType)
                    executeActor.send(ChangeCheckBoxState(memoRow, executeId.executionType))
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
            executeId.memoRow.changeTextColorByCheckBoxState(true, executeId.executionType)
        }

        setCheckedChangeAction(executeId)
    }
}

private fun addCheckBox(executeId: AddCheckBox) {
    Log.d("場所:addCheckBox", "checkBox追加処理に入った")

    val newCheckBox = createNewCheckBoxView(executeId)
    val memoRow = executeId.memoRow

    when (executeId.executionType) {
        is CreateNewMemo -> {
            editViewModel.updateMemoContents { memoContents ->
                val indexOfMemoRow = memoContents.indexOfFirst { it.memoRowId.value == memoRow.id }

                memoContainer.setConstraintForBulletsView(memoRow, newCheckBox, 80)

                Log.d("場所:addCheckBox", "変更前:size=${memoContents.size} memoContents=${memoContents}")

                memoContents.flatMap {
                    if (it.memoRowId.value == memoRow.id)
                        listOf(memoContents[indexOfMemoRow].copy(
                                checkBoxId = CheckBoxId(Some(newCheckBox.id)),
                                checkBoxState = CheckBoxState(false)
                            )).k()
                    else listOf(it).k()
                }
            }
            Log.d("場所:addCheckBox", "変更後:size=${editViewModel.getMemoContents().size} MemoContents=${editViewModel.getMemoContents()}")
        }
        else -> memoContainer.setConstraintForBulletsView(memoRow, newCheckBox, 80)
    }
}

private fun deleteCheckBox(executeId: DeleteCheckBox) {
    Log.d("場所:deleteCheckBox", "checkBox削除処理に入った")
    editViewModel.updateMemoContents { memoContents ->
        val memoRow = executeId.memoRow
        val indexOfMemoRow = memoContents.indexOfFirst { it.memoRowId.value == memoRow.id }
        val checkBoxId = memoContents[indexOfMemoRow].checkBoxId

        memoContainer.apply {
            setConstraintForDeleteBulletsView(memoRow)
            removeBulletsViewFromLayout(editFragment, memoRow, checkBoxId)
        }

        Log.d("場所:deleteCheckBox", "変更前:size=${memoContents.size} memoContents=${memoContents}")

        memoContents.flatMap {
            if (it.memoRowId.value == executeId.memoRow.id)
                listOf(memoContents[indexOfMemoRow].copy(
                    checkBoxId = CheckBoxId(None),
                    checkBoxState = CheckBoxState(false)
                )).k()
            else listOf(it).k()
        }
    }
    Log.d("場所:deleteCheckBox",
        "変更後:size=${editViewModel.getMemoContents().size} memoContents=${editViewModel.getMemoContents()}")
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

private fun addDot(executeId: AddDot) {
    Log.d("場所:addDot", "dot追加処理に入った")

    val newDot = createDotTextView(executeId)
    val memoRow = executeId.memoRow

    when (executeId.executionType) {
        is CreateNewMemo -> {
            editViewModel.updateMemoContents { memoContents ->
                memoContainer.setConstraintForBulletsView(memoRow, newDot, 80, 40)

                Log.d("場所:addDot", "変更前:size=${memoContents.size} memoContents=${memoContents}")

                val indexOfMemoRow = memoContents.indexOfFirst { it.memoRowId.value == memoRow.id }

                memoContents.flatMap {
                    if (it.memoRowId.value == executeId.memoRow.id)
                        listOf(memoContents[indexOfMemoRow].copy(dotId = DotId(Some(newDot.id)))).k()
                    else listOf(it).k()
                }
            }
            Log.d("場所:addDot",
                "変更後:size=${editViewModel.getMemoContents().size} memoContents=${editViewModel.getMemoContents()}")
        }
        else -> memoContainer.setConstraintForBulletsView(executeId.memoRow, newDot, 80, 40)
    }
}

private fun deleteDot(executeId: DeleteDot) {
    Log.d("場所:deleteDot", "dotの削除処理に入った")
    editViewModel.updateMemoContents { memoContents ->
        val memoRow = executeId.memoRow
        val indexOfMemoRow = memoContents.indexOfFirst { it.memoRowId.value == memoRow.id }
        val dotId = memoContents[indexOfMemoRow].dotId
        Log.d("場所:deleteDot", "変更前:size=${memoContents.size} memoContents=${memoContents}")

        memoContainer.apply {
            setConstraintForDeleteBulletsView(memoRow)
            removeBulletsViewFromLayout(editFragment, memoRow, dotId)
        }

        memoContents.flatMap {
            if (it.memoRowId.value == executeId.memoRow.id)
                listOf(memoContents[indexOfMemoRow].copy(dotId = DotId(None))).k()
            else listOf(it).k()
        }
    }
    Log.d("場所:deleteDot",
        "変更後:size=${editViewModel.getMemoContents().size} memoContents=${editViewModel.getMemoContents()}")
}

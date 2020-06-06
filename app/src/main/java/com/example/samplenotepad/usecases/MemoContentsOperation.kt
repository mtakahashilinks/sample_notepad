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
import com.example.samplenotepad.views.main.setTextAndCursorPosition
import com.example.samplenotepad.views.search.DisplayMemoFragment
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor


private lateinit var editFragment: MemoEditFragment
private lateinit var displayFragment: DisplayMemoFragment
private lateinit var editViewModel: MemoEditViewModel
private lateinit var searchViewModel: SearchViewModel
private lateinit var memoContainer: ConstraintLayout
private lateinit var executeActor: SendChannel<TypeForExecuteMemoContents>
private lateinit var formerMemoRowForExistMemo: MemoRow //databaseから読みだしたメモの編集や表示の際のみ使う


@ObsoleteCoroutinesApi
internal fun initMemoContentsOperation(
    fragment: Fragment, viewModel: ViewModel, container: ConstraintLayout, executionType: WhichMemoExecution
) = runBlocking {
    when (executionType){
        is CreateNewMemo -> {
            editFragment = fragment as MemoEditFragment
            editViewModel = viewModel as MemoEditViewModel
            executeActor = viewModel.viewModelScope.executeMemoOperation()
            memoContainer = container

            editViewModel.apply {
                updateMemoContents { listOf<MemoRowInfo>().k() }
                viewModelScope.launch {
                    executeActor.send(CreateFirstMemoRow(Text(""), CreateNewMemo))
                }.join()
                updateMemoContentsAtSavePoint()
            }
        }
        is EditExistMemo -> {
            editFragment = fragment as MemoEditFragment
            editViewModel = viewModel as MemoEditViewModel
            executeActor = viewModel.viewModelScope.executeMemoOperation()
            memoContainer = container

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
internal fun MemoRow.checkBoxOperation() {
    val memoContents = editViewModel.getMemoContents()
    val memoRowInfo = memoContents[memoContents.indexOfFirst { it.memoRowId.value == this.id }]
    val checkBoxId = memoRowInfo.checkBoxId.value

    editViewModel.viewModelScope.launch {
        when {
            memoRowInfo.dotId.value is Some<Int> -> {
                executeActor.send(DeleteDot(this@checkBoxOperation))
                executeActor.send(AddCheckBox(this@checkBoxOperation, CreateNewMemo))
            }
            checkBoxId is None -> executeActor.send(AddCheckBox(this@checkBoxOperation, CreateNewMemo))
            checkBoxId is Some<Int> -> executeActor.send(DeleteCheckBox(this@checkBoxOperation))
        }
    }
}

//ボタンがクリックされた時のdot処理の入り口
internal fun MemoRow.dotOperation() {
    val mList = editViewModel.getMemoContents()
    val memoRowInfo = mList[mList.indexOfFirst { it.memoRowId.value == this.id }]
    val dotId = memoRowInfo.dotId.value

    editViewModel.viewModelScope.launch {
        when {
            memoRowInfo.checkBoxId.value is Some<Int> -> {
                executeActor.send(DeleteCheckBox(this@dotOperation))
                executeActor.send(AddDot(this@dotOperation, CreateNewMemo))
            }
            dotId is None -> executeActor.send(AddDot(this@dotOperation, CreateNewMemo))
            dotId is Some<Int> -> executeActor.send(DeleteDot(this@dotOperation))
        }
    }
}

internal fun clearAll() {
    editViewModel.viewModelScope.launch {
        executeActor.send(ClearAll)
    }
}

internal fun saveMemo() {
    //フォーカスを外しすことでupdateTextOfMemoRowInfoが呼ばれてTextプロパティが更新される
    memoContainer.clearFocus()

    editViewModel.viewModelScope.launch {
        executeActor.send(SaveMemoInfo)
    }
}

private fun saveOperation() = runBlocking {
    Log.d("saveOperation", "save処理に入った")

    saveMemoInfo(editFragment, editViewModel, editViewModel.getMemoInfo(), editViewModel.getMemoContents())
}

private fun createMemoRowsForExistMemo(executionType: WhichMemoExecution, memoContents: MemoContents) {
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

    val scope = when (executionType) {
        is DisplayExistMemo -> searchViewModel.viewModelScope
        else -> editViewModel.viewModelScope
    }

    scope.launch {
        memoContents.toList().createFirstRow().createNextRow()
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
                is ChangeCheckBoxState -> changeCheckBoxState(msg)
                is AddDot -> addDot(msg)
                is DeleteDot -> deleteDot(msg)
                is ClearAll -> clearAllInMemoContents()
                is SaveMemoInfo -> saveOperation()
            }

            Log.d("場所:executeMemoOperation", "executeMemoOperationが終わった executeId=$msg")
        }


    }


private fun MemoRow.setEnterKeyAction() {
    this.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            Log.d("場所:beforeTextChanged", "s=$s start=$start  count=$count after=$after")
            if (this@setEnterKeyAction is MemoRow && this@setEnterKeyAction.selectionEnd != 0)
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
                Log.d("場所:setOnFocusChangeListener", "FocusChange(Lost)が呼ばれた")

                editViewModel.viewModelScope.launch { executeActor.send(UpdateTextOfMemoRowInfo(v)) }
            }
            hasFocus -> {
                Log.d("場所:setOnFocusChangeListener", "FocusChange(Get)が呼ばれた")

                editViewModel.updateIfAtFirstInText(true)
                Log.d("場所:setOnFocusChangeListener", "ifAtFirstInText=${editViewModel.getIfAtFirstInText()}")
            }
        }
    }
}

private fun EditText.setTouchAction() {
    setOnTouchListener { v, event ->
        Log.d("場所:setOnTouchListener", "setOnTouchListenerが呼ばれた")

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
        setTextAndCursorPosition(memoRowText, memoRowText.value.length)
    }

    fun setLayoutParamForEditText() =
        ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT
        )

    return when (whichExecute) {
        is CreateNewMemo -> {
            EditText(editFragment.context, null, 0, R.style.MemoEditTextStyle).apply {
                layoutParams = setLayoutParamForEditText()
                id = View.generateViewId()
                setActionsAndText()
            }
        }
        is EditExistMemo -> {
            EditText(editFragment.context, null, 0, R.style.MemoEditTextStyle).apply {
                layoutParams = setLayoutParamForEditText()
                id = memoRowId ?: throw(NullPointerException("memoRowId mast not be null"))
                setActionsAndText()
            }
        }
        is DisplayExistMemo -> {
            EditText(displayFragment.context, null, 0, R.style.MemoEditTextStyle).apply {
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
                            MemoRowId(targetMemoRowId)
                        )
                    }
                    maxIndexOfList > indexOfTargetMemoRow -> {
                        Log.d("場所:createNextMemoRow", "下に他のViewがある場合")
                        val nextMemoRowId = memoContents[indexOfTargetMemoRow + 1].memoRowId

                        memoContainer.setConstraintForNextMemoRowWithBelow(
                            newMemoRow,
                            MemoRowId(targetMemoRowId),
                            nextMemoRowId
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
            //これを最後にしないと無限ループになる
            newMemoRow.setTextAndCursorPosition(executeId.text)

            editViewModel.updateIfAtFirstInText(true)
            Log.d("場所:createNextMemoRow", "ifAtFirstInText=${editViewModel.getIfAtFirstInText()}")

            Log.d("場所:createNextMemoRow",
                "変更後:size=${editViewModel.getMemoContents().size} MemoContents=${editViewModel.getMemoContents()}")
        }
        else -> {
            memoContainer.setConstraintForNextMemoRowWithNoBelow(
                newMemoRow,
                MemoRowId(formerMemoRowForExistMemo.id)
            )

            if (executeId.memoRowInfo != null) {
                newMemoRow.addCheckBoxAndDot(executeId.memoRowInfo, executeId.executionType)
            }

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
        val textOfFormerMemoRow = formerMemoRow.text.toString()

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
                targetMemoRow,
                MemoRowId(formerMemoRowId),
                nextMemoRowId
            )
        }

        memoContainer.removeMemoRowFromLayout(editFragment, targetMemoRow, formerMemoRow)

        formerMemoRow.setTextAndCursorPosition(
            Text(textOfFormerMemoRow + targetMemoRow.text.toString()), textOfFormerMemoRow.length
        )

        memoContents.filter { it.memoRowId.value != targetMemoRow.id }.k()
    }
    Log.d("場所:deleteMemoRow",
        "変更後:size=${editViewModel.getMemoContents().size} MemoContents=${editViewModel.getMemoContents()}")
}


private fun CheckBox.setCheckedChangeAction(executeId: AddCheckBox) {
    this.setOnCheckedChangeListener { buttonView, isChecked ->
        val memoRow = executeId.memoRow
        Log.d("場所:setOnCheckedChangeListener", "targetMemoRowId=${memoRow.id}")
        editViewModel.viewModelScope.launch {
            when (isChecked) {
                true -> {
                    memoRow.setTextColor(resources.getColor(R.color.colorGray, editFragment.activity?.theme))

                    executeActor.send(ChangeCheckBoxState(memoRow))
                }
                false -> {
                    memoRow.setTextColor(resources.getColor(R.color.colorBlack, editFragment.activity?.theme))

                    executeActor.send(ChangeCheckBoxState(memoRow))
                }
            }
        }
        Log.d("場所:setOnCheckedChangeListener",
            "変更後:size=${editViewModel.getMemoContents().size} contentsList=${editViewModel.getMemoContents()}")
    }
}

private fun changeCheckBoxState(executeId: ChangeCheckBoxState) {
    editViewModel.updateMemoContents { memoContents ->
        val memoRow = executeId.memoRow
        val indexOfMemoRow = memoContents.indexOfFirst { it.memoRowId.value == memoRow.id }
        Log.d("場所:changeCheckBoxState", "変更前:size=${memoContents.size} memoContents=${memoContents}")

        memoContents.flatMap {
            if (it.memoRowId.value == executeId.memoRow.id)
                listOf(memoContents[indexOfMemoRow].copy(
                    checkBoxState = CheckBoxState(!it.checkBoxState.value)
                )).k()
            else listOf(it).k()
        }
    }
    Log.d("場所:changeCheckBoxState",
        "変更後:size=${editViewModel.getMemoContents().size} memoContents=${editViewModel.getMemoContents()}")
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
            is CreateNewMemo -> { View.generateViewId() }
            is EditExistMemo , is DisplayExistMemo -> {
                executeId.checkBoxId ?: throw(NullPointerException("CheckBoxId mast not be null"))
            }
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
                    if (it.memoRowId.value == executeId.memoRow.id)
                        listOf(memoContents[indexOfMemoRow].copy(
                                checkBoxId = CheckBoxId(Some(newCheckBox.id)),
                                checkBoxState = CheckBoxState(false)
                            )).k()
                    else listOf(it).k()
                }
            }
            Log.d("場所:addCheckBox",
                "変更後:size=${editViewModel.getMemoContents().size} MemoContents=${editViewModel.getMemoContents()}")
        }
        else -> {
            memoContainer.setConstraintForBulletsView(executeId.memoRow, newCheckBox, 80)

            if (executeId.checkBoxState) {
                newCheckBox.isChecked = true

                memoRow.setTextColor (
                    memoContainer.resources.getColor(R.color.colorGray, editFragment.activity?.theme)
                )
            }
        }
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
                memoContainer.setConstraintForBulletsView(memoRow, newDot, 60, 20)

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
        else -> memoContainer.setConstraintForBulletsView(executeId.memoRow, newDot, 60, 20)
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


private fun clearAllInMemoContents() {
    Log.d("場所:AllInMemoContents", "ClearAll処理に入った")

    memoContainer.removeAllViews()

    editViewModel.updateMemoContents { listOf<MemoRowInfo>().k() }
    Log.d("場所:clearAllInMemoContents", "memoContents=${editViewModel.getMemoContents()}")
}

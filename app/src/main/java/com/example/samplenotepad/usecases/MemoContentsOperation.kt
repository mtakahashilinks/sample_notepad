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
import androidx.lifecycle.viewModelScope
import arrow.core.*
import com.example.samplenotepad.*
import com.example.samplenotepad.data.saveMemoInfo
import com.example.samplenotepad.entities.*
import com.example.samplenotepad.viewModels.MemoInputViewModel
import com.example.samplenotepad.views.main.*
import com.example.samplenotepad.views.main.setConstraintForFirstMemoRow
import com.example.samplenotepad.views.main.setConstraintForNextMemoRowWithNoBelow
import com.example.samplenotepad.views.main.setTextAndCursorPosition
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor


private lateinit var inputFragment: MemoInputFragment
private lateinit var inputViewModel: MemoInputViewModel
private lateinit var memoContainer: ConstraintLayout
private lateinit var executeActor: SendChannel<TypeForExecuteMemoContents>


@ObsoleteCoroutinesApi
internal fun initMemoContentsOperation(fragment: MemoInputFragment, viewModel: MemoInputViewModel,
                                       container: ConstraintLayout, mMemoContents: Option<MemoContents>) {
    inputFragment = fragment
    inputViewModel = viewModel
    memoContainer = container
    executeActor = viewModel.viewModelScope.executeMemoOperation()

    when (mMemoContents){
        is Some ->  inputViewModel.memoContents.getAndSet(mMemoContents.t)
        is None -> {
            inputViewModel.viewModelScope.launch {
                inputViewModel.memoContents.getAndSet(listOf<MemoRowInfo>().k())
                executeActor.send(CreateFirstMemoRow(Text("")))
            }
        }
    }
}

internal fun closeMemoContentsOperation() {
    executeActor.close()
}

//ボタンがクリックされた時のcheckBox処理の入り口
internal fun MemoRow.operationCheckBox() {
    val memoContents = inputViewModel.memoContents.value
    val memoRowInfo = memoContents[memoContents.indexOfFirst { it.memoRowId.value == this.id }]
    val checkBoxId = memoRowInfo.checkBoxId.value

    inputViewModel.viewModelScope.launch {
        when {
            memoRowInfo.dotId.value is Some<Int> -> {
                executeActor.send(DeleteDot(this@operationCheckBox))
                executeActor.send(AddCheckBox(this@operationCheckBox))
            }
            checkBoxId is None -> executeActor.send(AddCheckBox(this@operationCheckBox))
            checkBoxId is Some<Int> -> executeActor.send(DeleteCheckBox(this@operationCheckBox))
        }
    }
}

//ボタンがクリックされた時のdot処理の入り口
internal fun MemoRow.dotOperation() {
    val mList = inputViewModel.memoContents.value
    val memoRowInfo = mList[mList.indexOfFirst { it.memoRowId.value == this.id }]
    val dotId = memoRowInfo.dotId.value

    inputViewModel.viewModelScope.launch {
        when {
            memoRowInfo.checkBoxId.value is Some<Int> -> {
                executeActor.send(DeleteCheckBox(this@dotOperation))
                executeActor.send(AddDot(this@dotOperation))
            }
            dotId is None -> executeActor.send(AddDot(this@dotOperation))
            dotId is Some<Int> -> executeActor.send(DeleteDot(this@dotOperation))
        }
    }
}

internal fun clearAll() {
    inputViewModel.viewModelScope.launch {
        executeActor.send(ClearAll())
        executeActor.send(CreateFirstMemoRow(Text("")))
    }
}

internal fun saveOperation(executeId: SaveMemoInfo) {
    Log.d("saveOperation", "save処理に入った")

    val focusView = memoContainer.findFocus()

    //フォーカスがあるViewがMemoRowならMemoInfoのTextプロパティを更新
    inputViewModel.viewModelScope.launch {
        if (focusView is MemoRow) executeActor.send(UpdateTextOfMemoRowInfo(focusView))
    }

    saveMemoInfo(inputFragment, inputViewModel, executeId.optionValues)
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
                is SaveMemoInfo -> saveOperation(msg)
            }

            Log.d("場所:executeMemoOperation", "executeMemoOperationが終わった executeId=$msg")
        }


    }


private fun MemoRow.setEnterKeyAction() {
    this.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            Log.d("場所:beforeTextChanged", "s=$s start=$start  count=$count after=$after")
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

                    inputViewModel.viewModelScope.launch {
                        executeActor.send(CreateNextMemoRow(Text(textBringToNextRow)))
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
                //delete処理
                code == KeyEvent.KEYCODE_DEL && inputViewModel.ifAtFirstInText.value -> {
                    val memoContents = inputViewModel.memoContents.value
                    val memoRowInfo =
                        memoContents[memoContents.indexOfFirst { it.memoRowId.value == v.id }]

                    Log.d("場所:setOnKeyListener", "Delキーイベントに入った")
                    Log.d("場所:setOnKeyListener", "削除するMemoRowのId=${v.id}")
                    Log.d("場所:setOnKeyListener", "selectionEnd=${v.selectionEnd}")
                    Log.d("場所:setOnKeyListener", "size=${memoContents.size} memoContents=${memoContents}")

                    inputViewModel.viewModelScope.launch {
                        when {
                            memoRowInfo.checkBoxId.value is Some -> executeActor.send(DeleteCheckBox(v))
                            memoRowInfo.dotId.value is Some -> executeActor.send(DeleteDot(v))
                        }

                        executeActor.send(DeleteMemoRow(v))
                    }

                    Log.d("場所:setOnKeyListener", "ifAtFirstInText=${inputViewModel.ifAtFirstInText.value}")
                }
                //このタイミングでフラグをtrueに変更しないと、カーソルが文頭に移動した瞬間に削除処理に入ってしまう
                // (カーソルが文頭に移動た後にDELキーを押した時点で削除処理に入ってほしい)
                v.selectionEnd == 0 ->
                    inputViewModel.ifAtFirstInText.compareAndSet(expect = false, update = true)
            }
        }
        false
    }
}

private fun MemoRow.setFocusChangeAction() {
    //フォーカスが他に移るタイミングでMemoRowInfoのTextを更新する
    this.setOnFocusChangeListener { v, hasFocus ->
        when {
            v is MemoRow && v.isClickable && !hasFocus -> {
                Log.d("場所:setOnFocusChangeListener", "FocusChange(Lost)が呼ばれた")

                inputViewModel.viewModelScope.launch {
                    executeActor.send(UpdateTextOfMemoRowInfo(v))
                }
            }
            v is MemoRow && hasFocus -> {
                Log.d("場所:setOnFocusChangeListener", "FocusChange(Get)が呼ばれた")
                val memoContents = inputViewModel.memoContents.value
                Log.d("場所:setOnFocusChangeListener", "FocusViewのId=${v.id}")
                Log.d("場所:setOnFocusChangeListener",
                    "FocusViewの位置=${memoContents.indexOfFirst { it.memoRowId.value == v.id } + 1}/${memoContents.size}")

                when (v.selectionEnd) {
                    0 -> {
                        inputViewModel.ifAtFirstInText.compareAndSet(expect = false, update = true)
                        Log.d("場所:setOnFocusChangeListener#0",
                            "ifAtFirstInText=${inputViewModel.ifAtFirstInText.value}")
                    }
                    else -> {
                        inputViewModel.ifAtFirstInText.compareAndSet(expect = true, update = false)
                        Log.d("場所:setOnFocusChangeListener#else",
                            "ifAtFirstInText=${inputViewModel.ifAtFirstInText.value}")
                    }
                }
            }
        }
    }
}


//MemoContentsの最初の行をセットする
private fun createFirstMemoRow(executeId: CreateFirstMemoRow) {
    Log.d("場所:createFirstMemoRow", "createFirstMemoRowに入った")
    Log.d("場所:createFirstMemoRow", "thread=${Thread.currentThread()}")

    inputViewModel.memoContents.updateAndGet { memoContents ->
        val text = executeId.text
        val newMemoRow = createNewMemoRowView(executeId).apply {
            setTextAndCursorPosition(text, text.value.length)
        }

        inputViewModel.viewModelScope.launch(Dispatchers.Main) {
            memoContainer.setConstraintForFirstMemoRow(newMemoRow)
        }

        Log.d("場所:createFirstMemoRow", "newMemoRowId=${newMemoRow.id}")
        Log.d("場所:createFirstMemoRow", "変更前:size=${memoContents.size} memoContents=${memoContents}")

        listOf(MemoRowInfo(MemoRowId(newMemoRow.id), text)).k()
    }
    Log.d("場所:createFirstMemoRow",
        "変更後:size=${inputViewModel.memoContents.value.size} memoContents=${inputViewModel.memoContents.value}")
}

private fun createNextMemoRow(executeId: CreateNextMemoRow) {
    Log.d("場所:createNextMemoRow", "createNextMemoRowに入った")
    val newMemoRow = createNewMemoRowView(executeId)

    inputViewModel.memoContents.updateAndGet { memoContents ->
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
                memoContainer.setConstraintForNextMemoRowWithNoBelow(newMemoRow, MemoRowId(targetMemoRowId))
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

    Log.d("場所:createNextMemoRow",
        "変更後:size=${inputViewModel.memoContents.value.size} MemoContents=${inputViewModel.memoContents.value}")
}

private fun deleteMemoRow(executeId: DeleteMemoRow) {
    Log.d("場所:deleteMemoRow", "deleteMemoRowに入った")

    inputViewModel.memoContents.updateAndGet { memoContents ->
        val targetMemoRow = executeId.memoRow
        val indexOfTargetMemoRow = memoContents.indexOfFirst { it.memoRowId.value == targetMemoRow.id }
        val maxIndexOfList = memoContents.size - 1
        val formerMemoRowId = memoContents[indexOfTargetMemoRow - 1].memoRowId.value
        val formerMemoRow = inputFragment.requireActivity().findViewById<EditText>(formerMemoRowId)
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

        memoContainer.removeMemoRowFromLayout(inputFragment, targetMemoRow, formerMemoRow)

        formerMemoRow.setTextAndCursorPosition(
            Text(textOfFormerMemoRow + targetMemoRow.text.toString()), textOfFormerMemoRow.length
        )

        memoContents.filter { it.memoRowId.value != targetMemoRow.id }.k()
    }
    Log.d("場所:deleteMemoRow",
        "変更後:size=${inputViewModel.memoContents.value.size} MemoContents=${inputViewModel.memoContents.value}")
}


private fun createNewMemoRowView(executeId: TypeForExecuteMemoContents): MemoRow {
    return EditText(inputFragment.context, null, 0, R.style.MemoEditTextStyle).apply {
        layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        id = View.generateViewId()
        setFocusChangeAction()
        setBackSpaceKeyAction(executeId)
        setEnterKeyAction()
    }
}


private fun CheckBox.setCheckedChangeAction(executeId: AddCheckBox) {
    this.setOnCheckedChangeListener { buttonView, isChecked ->
        val memoRow = executeId.memoRow

        inputViewModel.viewModelScope.launch {
            when (isChecked) {
                true -> {
                    memoRow.setTextColor(resources.getColor(R.color.colorGray, inputFragment.activity?.theme))

                    executeActor.send(ChangeCheckBoxState(memoRow))
                }
                false -> {
                    memoRow.setTextColor(resources.getColor(R.color.colorBlack, inputFragment.activity?.theme))

                    executeActor.send(ChangeCheckBoxState(memoRow))
                }
            }
        }
        Log.d("場所:setOnCheckedChangeListener",
            "変更後:size=${inputViewModel.memoContents.value.size} contentsList=${inputViewModel.memoContents.value}")
    }
}

private fun createNewCheckBoxView(executeId: AddCheckBox): CheckBox {
    return CheckBox(inputFragment.context).apply {
        layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            )
        ViewGroup.MarginLayoutParams(0, 0)
        id = View.generateViewId()
        textSize = 0f
        setPadding(4)
        setCheckedChangeAction(executeId)
    }
}

private fun changeCheckBoxState(executeId: ChangeCheckBoxState) {
    inputViewModel.memoContents.updateAndGet { memoContents ->
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
        "変更前:size=${inputViewModel.memoContents.value.size} memoContents=${inputViewModel.memoContents.value}")
}

private fun addCheckBox(executeId: AddCheckBox) {
    Log.d("場所:addCheckBox", "checkBox追加処理に入った")
    inputViewModel.memoContents.updateAndGet { memoContents ->
        val memoRow = executeId.memoRow
        val newCheckBox = createNewCheckBoxView(executeId)
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
        "変更後:size=${inputViewModel.memoContents.value.size} MemoContents=${inputViewModel.memoContents.value}")
}

private fun deleteCheckBox(executeId: DeleteCheckBox) {
    Log.d("場所:deleteCheckBox", "checkBox削除処理に入った")
    inputViewModel.memoContents.updateAndGet { memoContents ->
        val memoRow = executeId.memoRow
        val indexOfMemoRow = memoContents.indexOfFirst { it.memoRowId.value == memoRow.id }
        val checkBoxId = memoContents[indexOfMemoRow].checkBoxId

        memoContainer.apply {
            setConstraintForDeleteBulletsView(memoRow)
            removeBulletsViewFromLayout(inputFragment, memoRow, checkBoxId)
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
        "変更後:size=${inputViewModel.memoContents.value.size} memoContents=${inputViewModel.memoContents.value}")
}


private fun addDot(executeId: AddDot) {
    Log.d("場所:addDot", "dot追加処理に入った")
    inputViewModel.memoContents.updateAndGet { memoContents ->
        val memoRow = executeId.memoRow
        val indexOfMemoRow = memoContents.indexOfFirst { it.memoRowId.value == memoRow.id }
        val newDot = TextView(inputFragment.context).apply {
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            )
            id = View.generateViewId()
            setPadding(4)
            setBackgroundResource(R.color.colorTransparent)
            text = "・"
        }

        Log.d("場所:addDot", "変更前:size=${memoContents.size} memoContents=${memoContents}")

        memoContainer.setConstraintForBulletsView(memoRow, newDot, 60, 20)

        memoContents.flatMap {
            if (it.memoRowId.value == executeId.memoRow.id)
                listOf(memoContents[indexOfMemoRow].copy(dotId = DotId(Some(newDot.id)))).k()
            else listOf(it).k()
        }
    }
    Log.d("場所:addDot",
        "変更後:size=${inputViewModel.memoContents.value.size} memoContents=${inputViewModel.memoContents.value}")
}

private fun deleteDot(executeId: DeleteDot) {
    Log.d("場所:deleteDot", "dotの削除処理に入った")
    inputViewModel.memoContents.updateAndGet { memoContents ->
        val memoRow = executeId.memoRow
        val indexOfMemoRow = memoContents.indexOfFirst { it.memoRowId.value == memoRow.id }
        val dotId = memoContents[indexOfMemoRow].dotId
        Log.d("場所:deleteDot", "変更前:size=${memoContents.size} memoContents=${memoContents}")

        memoContainer.apply {
            setConstraintForDeleteBulletsView(memoRow)
            removeBulletsViewFromLayout(inputFragment, memoRow, dotId)
        }

        memoContents.flatMap {
            if (it.memoRowId.value == executeId.memoRow.id)
                listOf(memoContents[indexOfMemoRow].copy(dotId = DotId(None))).k()
            else listOf(it).k()
        }
    }
    Log.d("場所:deleteDot",
        "変更後:size=${inputViewModel.memoContents.value.size} memoContents=${inputViewModel.memoContents.value}")
}


private fun updateTextOfMemoRowInfo(executeId: UpdateTextOfMemoRowInfo) {
    inputViewModel.memoContents.updateAndGet { memoContents ->
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
        "変更後:size=${inputViewModel.memoContents.value.size} memoContents=${inputViewModel.memoContents.value}")
}


private fun clearAllInMemoContents() {
    Log.d("場所:clearAllInMemoContents", "ClearAll処理に入った")

    memoContainer.removeAllViews()

    inputViewModel.memoContents.getAndSet(listOf<MemoRowInfo>().k())
    Log.d("場所:clearAllInMemoContents", "memoContents=${inputViewModel.memoContents.value}")
}

package com.example.samplenotepad


import android.content.Context
import androidx.lifecycle.ViewModel
import arrow.core.internal.AtomicBooleanW
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.setPadding
import androidx.lifecycle.viewModelScope
import arrow.core.*
import arrow.core.extensions.listk.semigroupK.combineK
import arrow.core.internal.AtomicRefW
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch


class MemoMainViewModel : ViewModel() {

    //viewPagerのバグのためのとりあえずのobject
    object ForFirstFocusInMainFragment {
        private lateinit var mainFragment: MemoMainFragment
        private lateinit var memoContainer: ConstraintLayout

        internal fun setFragmentAndContainer(fragment: MemoMainFragment, container: ConstraintLayout) {
            mainFragment = fragment
            memoContainer = container
        }

        internal fun setFocusAndSoftWareKeyboard() {
            memoContainer.getChildAt(0).requestFocus()
            val inputManager =
                mainFragment.context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputManager.restartInput(memoContainer.focusedChild)
        }
    }


    object MemoContentsOperation {
        private lateinit var memoContainer: ConstraintLayout
        private lateinit var mainFragment: MemoMainFragment
        private lateinit var mainViewModel: MemoMainViewModel

        private val memoContents = AtomicRefW(MemoContents(listOf<MemoRowInfo>().k()))
        private val ifAtFirstInText = AtomicBooleanW(false) //DelKeyが押された時にMemoRowの削除処理に入るかどうかの判断基準


        //memoContentsの変更処理は全てここを通す事で、非同期でも衝突せずに順番に処理される
        private fun CoroutineScope.executeMemoOperationActor() = actor<ExecuteTypeForMemoContents> {
            Log.d("場所:executeMemoOperationActor", "executeMemoOperationActorに入った")

            val execution = mainViewModel.viewModelScope.launch {
                channel.receiveAsFlow()
                    .buffer(Channel.RENDEZVOUS)
                    .collect { executeMemoOperation(it) }
            }
            execution.join()
        }

        private fun executeMemoOperation(executeId: ExecuteTypeForMemoContents) {
            Log.d("場所:executeMemoOperation", "executeMemoOperationに入った executeId=$executeId")
            when (executeId) {
                is UpdateTextOfMemoRowInfo -> updateTextOfMemoRowInfo(executeId)
                is CreateFirstMemoRow -> createFirstMemoRow(executeId)
                is CreateNextMemoRow -> createNextMemoRow(executeId)
                is DeleteMemoRow -> deleteMemoRow(executeId)
                is AddCheckBox -> addCheckBox(executeId)
                is DeleteCheckBox -> deleteCheckBox(executeId)
                is ChangeCheckBoxState -> changeCheckBoxState(executeId)
                is AddDot -> addDot(executeId)
                is DeleteDot -> deleteDot(executeId)
                is ClearAll -> clearAllInMemoContents()
            }
            Log.d("場所:executeMemoOperation", "executeMemoOperationが終わった executeId=$executeId")
        }


        internal fun initMemoContentsOperation(fragment: MemoMainFragment,
                                               viewModel: MemoMainViewModel,
                                               container: ConstraintLayout,
                                               mMemoContents: Option<MemoContents>) {
                mainFragment = fragment
                mainViewModel = viewModel
                memoContainer = container

                when (mMemoContents){
                    is Some ->  memoContents.getAndSet(mMemoContents.t)
                    is None -> {
                        memoContents.getAndSet(MemoContents(listOf<MemoRowInfo>().k()))
                        createFirstMemoRow(CreateFirstMemoRow(Text("")))
                    }
                }
            }

        private fun MemoRow.setTextAndCursorPosition(text: Text, selection: Int = 0) {
            Log.d("場所:setTextAndCursorPosition", "setTextAndCursorPositionに入った")
            this.apply {
                setText(text.value)
                requestFocus()
                setSelection(selection)
            }
        }


        private fun updateTextOfMemoRowInfo(executeId: UpdateTextOfMemoRowInfo) {
            memoContents.updateAndGet { mMemoContents ->
                val mList = mMemoContents.contentsList
                val indexOfMemoRow = mList.indexOfFirst { it.memoRowId.value == executeId.memoRow.id }

                Log.d(
                    "場所:updateTextOfMemoContents",
                    "変更前:size=${mList.size} MemoContents=${mList}"
                )

                MemoContents(mList.flatMap {
                    if (it.memoRowId.value == executeId.memoRow.id)
                        listOf(mList[indexOfMemoRow].copy(text = Text(executeId.memoRow.text.toString()))).k()
                    else listOf(it).k()
                })
            }
            Log.d("場所:updateTextOfMemoContents",
                "変更後:size=${memoContents.value.contentsList.size} MemoContents=${memoContents.value.contentsList}")
        }

        //メモコンテンツの最初の行をセットする
        private fun createFirstMemoRow(executeId: CreateFirstMemoRow) {
            Log.d("場所:createFirstMemoRow", "createFirstMemoRowに入った")
            memoContents.updateAndGet { mMemoContents ->
                val mList = mMemoContents.contentsList
                val text = executeId.text
                val newMemoRow = createNewMemoRowView(executeId).apply {
                    setTextAndCursorPosition(text, text.value.length)
                }

                memoContainer.setConstraintForFirstMemoRow(newMemoRow)

                Log.d("場所:createFirstMemoRow", "newMemoRowId=${newMemoRow.id}")
                Log.d("場所:createFirstMemoRow", "変更前:size=${mList.size} MemoContents=${mList}")

                MemoContents(listOf(MemoRowInfo(MemoRowId(newMemoRow.id), text)).k())
            }
            Log.d("場所:createFirstMemoRow",
                "変更後:size=${memoContents.value.contentsList.size} MemoContents=${memoContents.value.contentsList}")
        }

        private fun createNextMemoRow(executeId: CreateNextMemoRow) {
            Log.d("場所:createNextMemoRow", "createNextMemoRowに入った")
            val newMemoRow = createNewMemoRowView(executeId)

            memoContents.updateAndGet { mMemoContents ->
                val mList = mMemoContents.contentsList
                val targetMemoRowId = memoContainer.findFocus().id
                val indexOfTargetMemoRow = mList.indexOfFirst { it.memoRowId.value == targetMemoRowId }
                val maxIndexOfList = mList.size - 1

                Log.d("場所:createNextMemoRow", "text=${executeId.text}")
                Log.d("場所:createNextMemoRow", "targetMemoRowId=$targetMemoRowId")
                Log.d("場所:createNextMemoRow", "newMemoRowId=${newMemoRow.id}")
                Log.d("場所:createNextMemoRow", "indexOfTargetMemoRow=$indexOfTargetMemoRow")
                Log.d("場所:createNextMemoRow", "maxIndexOfList=$maxIndexOfList")

                when {
                    maxIndexOfList == indexOfTargetMemoRow -> {
                        Log.d("場所:createNextMemoRow", "下に他のViewがない場合")
                        //TextViewとContainerViewの制約をセット
                        memoContainer.setConstraintForNextMemoRowWithNoBelow(
                            newMemoRow, MemoRowId(targetMemoRowId)
                        )
                    }
                    maxIndexOfList > indexOfTargetMemoRow -> {
                        Log.d("場所:createNextMemoRow", "下に他のViewがある場合")
                        val nextMemoRowId = mList[indexOfTargetMemoRow + 1].memoRowId

                        memoContainer.setConstraintForNextMemoRowWithBelow(
                            newMemoRow, MemoRowId(targetMemoRowId), nextMemoRowId
                        )
                    }
                }

                Log.d("場所:createNextMemoRow", "変更前:size=${mList.size} MemoContents=${mList}")

                when {
                    indexOfTargetMemoRow + 1 < (mList.size - 1) -> {
                        Log.d("場所:createNextMemoRow", "indexがリストサイズより小さい場合")
                        val beforeList = mList.take(indexOfTargetMemoRow + 1).k()
                        val afterList = mList.drop(indexOfTargetMemoRow + 1).k()

                        MemoContents(
                            beforeList.combineK(
                                listOf(MemoRowInfo(MemoRowId(newMemoRow.id))).k()
                            ).combineK(afterList)
                        )
                    }
                    else -> {
                        Log.d("場所:createNextMemoRow", "indexがリストの最後尾の場合")
                        MemoContents(mList.combineK(listOf(MemoRowInfo(MemoRowId(newMemoRow.id))).k()))
                    }
                }
            }
            Log.d("場所:createNextMemoRow",
                "変更後:size=${memoContents.value.contentsList.size} MemoContents=${memoContents.value.contentsList}")

            newMemoRow.setTextAndCursorPosition(executeId.text)
        }

        private fun deleteMemoRow(executeId: DeleteMemoRow) {
            Log.d("場所:deleteMemoRow", "deleteMemoRowに入った")

            memoContents.updateAndGet { mMemoContents ->
                val mList = mMemoContents.contentsList
                val targetMemoRow = executeId.memoRow
                val indexOfTargetMemoRow = mList.indexOfFirst { it.memoRowId.value == targetMemoRow.id }
                val maxIndexOfList = mList.size - 1
                val formerMemoRowId = mList[indexOfTargetMemoRow - 1].memoRowId.value
                val formerMemoRow = mainFragment.requireActivity().findViewById<EditText>(formerMemoRowId)
                val textOfFormerMemoRow = formerMemoRow.text.toString()

                Log.d("場所:deleteMemoRow", "targetMemoRowId=${targetMemoRow.id}")
                Log.d("場所:deleteMemoRow", "indexOfTargetMemoRow=$indexOfTargetMemoRow")
                Log.d("場所:deleteMemoRow", "maxIndexOfList=$maxIndexOfList")
                Log.d("場所:deleteMemoRow", "変更前:size=${mList.size} MemoContents=${mList}")

                //targetMemoRowの下に他のViewがある場合の制約のセット。無い場合はそのままViewを削除する。
                if (maxIndexOfList > indexOfTargetMemoRow) {
                    Log.d("場所:deleteMemoRow", "下に他のViewがある場合")
                    val nextMemoRowId = mList[indexOfTargetMemoRow + 1].memoRowId
                    memoContainer.setConstraintForDeleteMemoRow(
                        targetMemoRow, MemoRowId(formerMemoRowId), nextMemoRowId
                    )
                }

                memoContainer.removeMemoRowFromLayout(mainFragment, targetMemoRow, formerMemoRow)

                //FocusChangedListenerで処理をさせない為。プロパティの種類は何でも良い
                targetMemoRow.isClickable = false

                formerMemoRow.setTextAndCursorPosition(
                    Text(textOfFormerMemoRow + targetMemoRow.text.toString()), textOfFormerMemoRow.length
                )

                MemoContents(mList.filter { it.memoRowId.value != targetMemoRow.id }.k())
            }
            Log.d("場所:deleteMemoRow",
                "変更後:size=${memoContents.value.contentsList.size} MemoContents=${memoContents.value.contentsList}")
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

                            mainViewModel.viewModelScope.launch {
                                executeMemoOperationActor().apply {
                                    send(CreateNextMemoRow(Text(textBringToNextRow)))
                                    close()
                                }
                            }
                        }
                        else -> return
                    }
                }
            } )
        }

        private fun MemoRow.setBackSpaceKeyAction(executeId: ExecuteTypeForMemoContents) {
            this.setOnKeyListener { v, code, event ->
                if (executeId is CreateNextMemoRow) {
                    //まず文章の先頭でないのにFlagがtrueになっている場合にfalseに変更する
                    if (v is MemoRow && v.selectionEnd != 0)
                        ifAtFirstInText.compareAndSet(expect = true, update = false)

                    when {
                        code == KeyEvent.KEYCODE_DEL && ifAtFirstInText.value -> {
                            val mList = memoContents.value.contentsList
                            val memoRowInfo = mList[mList.indexOfFirst { it.memoRowId.value == v.id }]

                            Log.d("場所:setOnKeyListener", "Delキーイベントに入った")
                            Log.d("場所:setOnKeyListener", "削除するMemoRowのId=${(v as MemoRow).id}")
                            Log.d("場所:setOnKeyListener", "selectionEnd=${v.selectionEnd}")
                            Log.d("場所:setOnKeyListener", "size=${mList.size} MemoContents=${mList}")

                            when {
                                memoRowInfo.checkBoxId.value is Some ->
                                    executeMemoOperation(DeleteCheckBox(v))
                                memoRowInfo.dotId.value is Some ->
                                    executeMemoOperation(DeleteDot(v))
                            }

                            mainViewModel.viewModelScope.launch {
                                executeMemoOperationActor().apply {
                                    send(DeleteMemoRow(v))
                                    close()
                                }
                            }
                        }

                        //Delキー処理の後にフラグをtrueに変更することで、削除処理のタイミングを適正にしている
                        v is MemoRow && v.selectionEnd == 0 -> {
                            ifAtFirstInText.compareAndSet(expect = false, update = true)
                            Log.d("場所:setOnKeyListener#Frag変更処理", "atFirstInText=${ifAtFirstInText.value}")
                        }
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

                        mainViewModel.viewModelScope.launch {
                            executeMemoOperationActor().apply {
                                send(UpdateTextOfMemoRowInfo(v))
                                close()
                            }
                        }
                    }
                    v is MemoRow && hasFocus -> {
                        Log.d("場所:setOnFocusChangeListener", "FocusChange(Get)が呼ばれた")
                        val mList = memoContents.value.contentsList
                        Log.d("場所:setOnFocusChangeListener",
                            "FocusViewの位置=${mList.indexOfFirst { it.memoRowId.value == v.id } + 1}/${mList.size}")
                        when (v.selectionEnd) {
                            0 -> ifAtFirstInText.compareAndSet(expect = false, update = true)
                            else -> ifAtFirstInText.compareAndSet(expect = true, update = false)
                        }
                    }
                }
            }
        }

        private fun createNewMemoRowView(executeId: ExecuteTypeForMemoContents): MemoRow {
            return EditText(mainFragment.context, null, 0, R.style.MemoEditTextStyle).apply {
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


        //ボタンがクリックされた時のcheckBox処理の入り口
        internal fun MemoRow.operationCheckBox() {
            val mList = memoContents.value.contentsList
            val memoRowInfo = mList[mList.indexOfFirst { it.memoRowId.value == this.id }]
            val checkBoxId = memoRowInfo.checkBoxId.value

            mainViewModel.viewModelScope.launch {
                when {
                    memoRowInfo.dotId.value is Some<Int> -> {
                        executeMemoOperationActor().apply {
                            send(DeleteDot(this@operationCheckBox))
                            send(AddCheckBox(this@operationCheckBox))
                            close()
                        }
                    }
                    checkBoxId is None -> executeMemoOperationActor().apply {
                        send(AddCheckBox(this@operationCheckBox))
                        close()
                    }
                    checkBoxId is Some<Int> -> executeMemoOperationActor().apply {
                        send(DeleteCheckBox(this@operationCheckBox))
                        close()
                    }
                }
            }
        }


        private fun addCheckBox(executeId: AddCheckBox) {
            Log.d("場所:addCheckBox", "checkBox追加処理に入った")
            memoContents.updateAndGet { mMemoContents ->
                val mList = mMemoContents.contentsList
                val memoRow = executeId.memoRow
                val newCheckBox = createNewCheckBoxView(executeId)
                val indexOfMemoRow = mList.indexOfFirst { it.memoRowId.value == memoRow.id }

                memoContainer.setConstraintForBulletsView(memoRow, newCheckBox, 80)

                Log.d("場所:addCheckBox", "変更前:size=${mList.size} MemoContents=${mList}")

                MemoContents(mList.flatMap {
                    if (it.memoRowId.value == executeId.memoRow.id)
                        listOf(mList[indexOfMemoRow].copy(
                            checkBoxId = CheckBoxId(Some(newCheckBox.id)),
                            checkBoxState = CheckBoxState(false)
                        )).k()
                    else listOf(it).k()
                })
            }
            Log.d("場所:addCheckBox",
                "変更後:size=${memoContents.value.contentsList.size} MemoContents=${memoContents.value.contentsList}")
        }

        private fun deleteCheckBox(executeId: DeleteCheckBox) {
            Log.d("場所:deleteCheckBox", "checkBox削除処理に入った")
            memoContents.updateAndGet { mMemoContents ->
                val mList = mMemoContents.contentsList
                val memoRow = executeId.memoRow
                val indexOfMemoRow = mList.indexOfFirst { it.memoRowId.value == memoRow.id }
                val checkBoxId = mList[indexOfMemoRow].checkBoxId

                memoContainer.apply {
                    setConstraintForDeleteBulletsView(memoRow)
                    removeBulletsViewFromLayout(mainFragment, memoRow, checkBoxId)
                }

                Log.d("場所:deleteCheckBox", "変更前:size=${mList.size} MemoContents=${mList}")

                MemoContents(mList.flatMap {
                    if (it.memoRowId.value == executeId.memoRow.id)
                        listOf(mList[indexOfMemoRow].copy(
                            checkBoxId = CheckBoxId(None),
                            checkBoxState = CheckBoxState(false)
                        )).k()
                    else listOf(it).k()
                })
            }
            Log.d("場所:deleteCheckBox",
                "変更後:size=${memoContents.value.contentsList.size} MemoContents=${memoContents.value.contentsList}")
        }

        private fun createNewCheckBoxView(executeId: AddCheckBox): CheckBox {
            return CheckBox(mainFragment.context).apply {
                layoutParams =
                    ConstraintLayout.LayoutParams(
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

        private fun CheckBox.setCheckedChangeAction(executeId: AddCheckBox) {
            this.setOnCheckedChangeListener { buttonView, isChecked ->
                val memoRow = executeId.memoRow

                mainViewModel.viewModelScope.launch {
                    when (isChecked) {
                        true -> {
                            memoRow.setTextColor(
                                resources.getColor(R.color.colorGray, mainFragment.activity?.theme)
                            )

                            executeMemoOperationActor().apply {
                                send(ChangeCheckBoxState(memoRow))
                                close()
                            }
                        }
                        false -> {
                            memoRow.setTextColor(
                                resources.getColor(R.color.colorBlack, mainFragment.activity?.theme)
                            )

                            executeMemoOperationActor().apply {
                                send(ChangeCheckBoxState(memoRow))
                                close()
                            }
                        }
                    }
                }
                Log.d("場所:setOnCheckedChangeListener",
                    "変更後:size=${memoContents.value.contentsList.size} MemoContents=${memoContents.value.contentsList}")
            }
        }

        private fun changeCheckBoxState(executeId: ChangeCheckBoxState) {
            memoContents.updateAndGet { mMemoContents ->
                val mList = mMemoContents.contentsList
                val memoRow = executeId.memoRow
                val indexOfMemoRow = mList.indexOfFirst { it.memoRowId.value == memoRow.id }
                Log.d("場所:changeCheckBoxState", "変更前:size=${mList.size} MemoContents=${mList}")

                MemoContents(mList.flatMap {
                    if (it.memoRowId.value == executeId.memoRow.id)
                        listOf(mList[indexOfMemoRow].copy(
                            checkBoxState = CheckBoxState(!it.checkBoxState.value))
                        ).k()
                    else listOf(it).k()
                })
            }
            Log.d("場所:changeCheckBoxState",
                "変更前:size=${memoContents.value.contentsList.size} MemoContents=${memoContents.value.contentsList}")
        }


        //ボタンがクリックされた時のdot処理の入り口
        internal fun MemoRow.dotOperation() {
            val mList = memoContents.value.contentsList
            val memoRowInfo = mList[mList.indexOfFirst { it.memoRowId.value == this.id }]
            val dotId = memoRowInfo.dotId.value

            mainViewModel.viewModelScope.launch {
                when {
                    memoRowInfo.checkBoxId.value is Some<Int> -> {
                        executeMemoOperationActor().apply {
                            send(DeleteCheckBox(this@dotOperation))
                            send(AddDot(this@dotOperation))
                            close()
                        }
                    }
                    dotId is None -> executeMemoOperationActor().apply {
                        send(AddDot(this@dotOperation))
                        close()
                    }
                    dotId is Some<Int> -> executeMemoOperationActor().apply {
                        send(DeleteDot(this@dotOperation))
                        close()
                    }
                }
            }
        }

        private fun addDot(executeId: AddDot) {
            Log.d("場所:addDot", "dot追加処理に入った")
            memoContents.updateAndGet { mMemoContents ->
                val mList = mMemoContents.contentsList
                val memoRow = executeId.memoRow
                val indexOfMemoRow = mList.indexOfFirst { it.memoRowId.value == memoRow.id }
                val newDot = TextView(mainFragment.context).apply {
                    layoutParams = ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.WRAP_CONTENT,
                        ConstraintLayout.LayoutParams.WRAP_CONTENT
                    )
                    id = View.generateViewId()
                    setPadding(4)
                    setBackgroundResource(R.color.colorTransparent)
                    text = "・"
                }

                Log.d("場所:addDot", "変更前:size=${mList.size} MemoContents=${mList}")

                memoContainer.setConstraintForBulletsView(memoRow, newDot, 60, 20)

                MemoContents(mList.flatMap {
                    if (it.memoRowId.value == executeId.memoRow.id)
                        listOf(mList[indexOfMemoRow].copy(dotId = DotId(Some(newDot.id)))).k()
                    else listOf(it).k()
                })
            }
            Log.d("場所:addDot",
                "変更後:size=${memoContents.value.contentsList.size} MemoContents=${memoContents.value.contentsList}")
        }

        private fun deleteDot(executeId: DeleteDot) {
            Log.d("場所:deleteDot", "dotの削除処理に入った")
            memoContents.updateAndGet { mMemoContents ->
                val mList = mMemoContents.contentsList
                val memoRow = executeId.memoRow
                val indexOfMemoRow = mList.indexOfFirst { it.memoRowId.value == memoRow.id }
                val dotId = mList[indexOfMemoRow].dotId
                Log.d("場所:deleteDot", "変更前:size=${mList.size} MemoContents=${mList}")

                memoContainer.apply {
                    setConstraintForDeleteBulletsView(memoRow)
                    removeBulletsViewFromLayout(mainFragment, memoRow, dotId)
                }

                MemoContents(mList.flatMap {
                    if (it.memoRowId.value == executeId.memoRow.id)
                        listOf(mList[indexOfMemoRow].copy(dotId = DotId(None))).k() else listOf(it).k()
                })
            }
            Log.d("場所:deleteDot",
                "変更後:size=${memoContents.value.contentsList.size} MemoContents=${memoContents.value.contentsList}")
        }


        internal fun clearAll() {
            mainViewModel.viewModelScope.launch {
                executeMemoOperationActor().apply {
                    send(ClearAll())
                    send(CreateFirstMemoRow(Text("")))
                    close()
                }
            }
        }

        private fun clearAllInMemoContents() {
            Log.d("場所:clearAllInMemoContents", "ClearAll処理に入った")

            memoContainer.removeAllViews()
            memoContents.getAndSet(MemoContents(listOf<MemoRowInfo>().k()))
            Log.d("場所:clearAllInMemoContents", "memoContents=${memoContents.value.contentsList}")
            ifAtFirstInText.compareAndSet(expect = false, update = true)
        }
    }


    override fun onCleared() {
        super.onCleared()
        //ここに終了処理を書く
    }
}

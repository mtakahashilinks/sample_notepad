package com.example.samplenotepad

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import arrow.core.internal.AtomicBooleanW
import android.graphics.Color
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
import arrow.core.None
import arrow.core.Some
import arrow.core.k


class MemoMainViewModel : ViewModel() {

    //ここからMainMemoFragment用のPropertyとMethod

    private lateinit var container: ConstraintLayout
    private var memoContents = MemoContents(listOf<MemoRowInfo>().k())
    //EditTextのbackSpace処理に入るかどうかのフラグ
    private val ifAtFirstInText = AtomicBooleanW(false)

    internal fun getMemoContents(): MemoContents {
        return memoContents
    }

    private fun addToMemoContents(index: Int, memoRowInfo: MemoRowInfo) {
        val copiedList = mutableListOf<MemoRowInfo>().apply {
            addAll(memoContents.contentsList.copy())
            add(index, memoRowInfo)
        }.k()
        memoContents = MemoContents(copiedList)
    }

    private fun modifyMemoRowInfo(memoRow: MemoRow, memoRowInfo: MemoRowInfo) {
        val index = getMemoRowIndexInList(MemoRowId(memoRow.id))
        val copiedList = mutableListOf<MemoRowInfo>().apply {
            addAll(memoContents.contentsList.copy())
            set(index, memoRowInfo)
        }.k()
        memoContents = MemoContents(copiedList)
    }

    private fun deleteFromMemoContents(memoRowId: MemoRowId) {
        val index = getMemoRowIndexInList(memoRowId)
        val copiedList = mutableListOf<MemoRowInfo>().apply {
            addAll(memoContents.contentsList.copy())
            removeAt(index)
        }.k()
        memoContents = MemoContents(copiedList)
    }

    internal fun getMemoRowIndexInList(targetMemoRowId: MemoRowId): Int {
        return memoContents.contentsList.indexOfFirst {
            it.memoRowId.value == targetMemoRowId.value
        }
    }

    //ここまでMainMemoFragment用のPropertyとMethod


    //ここからOptionMemoFragment用のPropertyとMethod
    val memoTitle = MutableLiveData<String>()
    val memoCategory = MutableLiveData<String>()
    val memoCategoriesList = MutableLiveData<MutableList<String>>()
    val memoDate = MutableLiveData<String>()
    val memoTime = MutableLiveData<String>()

    internal fun initMemoDate() {
        memoDate.value = getCurrentDay()
    }

    internal fun setMemoDate(date: String) {
        memoDate.value = date
    }

    //ここまでOptionMemoFragment用のPropertyとMethod


    object Queue4MemoContents {
        private val queue = mutableListOf<ExecuteTypeForMemoContentsQueue>()
        //trueの時のみmemoContentsの変更処理を許可するトランザクション処理のためのフラグ
        private val flagForQueue = AtomicBooleanW(true)

        private fun push(executeId: ExecuteTypeForMemoContentsQueue) = queue.add(executeId)
        private fun pop() = queue.drop(1)

        internal fun execute(viewModel: MemoMainViewModel, executeId: ExecuteTypeForMemoContentsQueue) {
            Log.d("場所:Queue4MemoContents#excute", "executeに入った")

            when (flagForQueue.compareAndSet(expect = true, update = false)) {
                true -> {
                    Log.d("場所:Queue4MemoContents#excute", "flagForQueueがtrueの場合")
                    when (executeId) {
                        is CreateFirstMemoRow -> viewModel.createFirstMemoRow(executeId)
                        is CreateNextMemoRow -> viewModel.createNextMemoRow(executeId)
                        is DeleteMemoRow -> viewModel.deleteMemoRow(executeId)
                        is AddCheckBox -> viewModel.addCheckBox(executeId)
                        is DeleteCheckBox -> viewModel.deleteCheckBox(executeId)
                        is AddBullet -> viewModel.addBullet(executeId)
                        is DeleteBullet -> viewModel.deleteBullet(executeId)
                        is ClearAll -> viewModel.clearAllInMemoContents()
                        is Complete -> return
                    }
                }
                false -> {
                    Log.d("場所:Queue4MemoContents#excute", "flagForQueueがfalseの場合")
                    when {
                        executeId is Complete && queue.isNotEmpty() -> {
                            Log.d("場所:Queue4MemoContents#excute",
                                "flagForQueueがfalseで実行中だった処理が終了した場合")
                            val mExecuteId = queue[0]

                            pop()
                            flagForQueue.value = true

                            when (mExecuteId) {
                                is CreateFirstMemoRow -> viewModel.createFirstMemoRow(mExecuteId)
                                is CreateNextMemoRow -> viewModel.createNextMemoRow(mExecuteId)
                                is DeleteMemoRow -> viewModel.deleteMemoRow(mExecuteId)
                                is AddCheckBox -> viewModel.addCheckBox(mExecuteId)
                                is DeleteCheckBox -> viewModel.deleteCheckBox(mExecuteId)
                                is AddBullet -> viewModel.addBullet(mExecuteId)
                                is DeleteBullet -> viewModel.deleteBullet(mExecuteId)
                                is ClearAll -> viewModel.clearAllInMemoContents()
                            }

                        }
                        executeId is Complete && queue.isEmpty() -> {
                            Log.d("場所:Queue4MemoContents#excute",
                                "flagForQueueがfalseで実行中だった処理が終了し、且つQueueが空の場合")
                            flagForQueue.value = true
                        }
                        executeId is CreateFirstMemoRow -> {
                            Log.d("場所:Queue4MemoContents#excute", "flagForQueueがfalseでqueueされる場合")
                            push(executeId)
                        }
                        executeId is CreateNextMemoRow -> push(executeId)
                        executeId is DeleteMemoRow -> push(executeId)
                        executeId is AddCheckBox -> push(executeId)
                        executeId is DeleteCheckBox -> push(executeId)
                        executeId is AddBullet -> push(executeId)
                        executeId is DeleteBullet -> push(executeId)
                        executeId is ClearAll -> push(executeId)
                        else -> return
                    }
                }
            }
        }
    }

    //メモコンテンツの最初の行をセットする
    private fun createFirstMemoRow(executeId: CreateFirstMemoRow) {
        Log.d("場所:createFirstMemoRow", "createFirstMemoRowに入った")
        this.apply {
            container = executeId.container

            val text = executeId.text
            val newMemoRow = createNewMemoRow(executeId.fragment).apply {
                setConstraintForFirstMemoRow(container)
                setTextAndCursorPosition(text, text.value.length)
            }
            Log.d("場所:createFirstMemoRow", "newMemoRowId=${newMemoRow.id}")

            addToMemoContents(0, MemoRowInfo(MemoRowId(newMemoRow.id), text))
            Log.d("場所:createFirstMemoRow", "memoContents=${getMemoContents()}")

            Queue4MemoContents.execute(this, Complete())
        }
    }

    private fun createNextMemoRow(executeId: CreateNextMemoRow) {
        Log.d("場所:createNextMemoRow", "createNextMemoRowに入った")
        Log.d("場所:createNextMemoRow", "text=${executeId.text}")
        this.apply {
            val targetMemoRowId = container.findFocus().id
            val targetMemoRowIndexInList = getMemoRowIndexInList(MemoRowId(targetMemoRowId))
            val maxIndexOfList = getMemoContents().contentsList.size -1
            val newMemoRow = createNewMemoRow(executeId.fragment).apply {
                setBackSpaceKeyAction(this@MemoMainViewModel, executeId)
            }

            Log.d("場所:createNextMemoRow", "targetMemoRowId=$targetMemoRowId")
            Log.d("場所:createNextMemoRow", "targetMemoRowIndexInList=$targetMemoRowIndexInList")
            Log.d("場所:createNextMemoRow", "maxIndexOfList=$maxIndexOfList")
            Log.d("場所:createNextMemoRow", "newMemoRowId=${newMemoRow.id}")

            newMemoRow.apply {
                when {
                    //FocusViewの下に他のViewが無い場合
                    maxIndexOfList == targetMemoRowIndexInList -> {
                        Log.d("場所:createNextMemoRow", "下に他のViewがない場合")
                        //TextViewとContainerViewの制約をセット
                        this.setConstraintForNextMemoRowWithNoBelow(
                            container, MemoRowId(targetMemoRowId)
                        )
                    }

                    //FocusViewの下に他のViewがある場合
                    maxIndexOfList > targetMemoRowIndexInList -> {
                        Log.d("場所:createNextMemoRow", "下に他のViewがある場合")
                        val nextMemoRowId =
                            getMemoContents().contentsList[targetMemoRowIndexInList + 1].memoRowId
                        this.setConstraintForNextMemoRowWithBelow(
                            container, MemoRowId(targetMemoRowId), nextMemoRowId
                        )
                    }
                }

                setTextAndCursorPosition(executeId.text)
                addToMemoContents(targetMemoRowIndexInList + 1, MemoRowInfo(MemoRowId(newMemoRow.id)))
            }

            Queue4MemoContents.execute(this, Complete())
        }
    }

    private fun deleteMemoRow(executeId: DeleteMemoRow) {
        Log.d("場所:deleteMemoRow", "deleteMemoRowに入った")
        this.apply {
            val memoRow = executeId.memoRow
            val targetMemoRowIndexInList = getMemoRowIndexInList(MemoRowId(memoRow.id))
            val maxIndexOfList = getMemoContents().contentsList.size - 1
            val formerMemoRowId =
                getMemoContents().contentsList[targetMemoRowIndexInList - 1].memoRowId.value
            val formerMemoRow = executeId.fragment.requireActivity().findViewById<EditText>(formerMemoRowId)
            val textOfFormerMemoRow = formerMemoRow.text.toString()
            val textOfTargetMemoRow = memoRow.text.toString()

            Log.d("場所:deleteMemoRow", "targetMemoRowId=${memoRow.id}")
            Log.d("場所:deleteMemoRow", "targetMemoRowIndexInList=$targetMemoRowIndexInList")
            Log.d("場所:deleteMemoRow", "maxIndexOfList=$maxIndexOfList")
            Log.d("場所:deleteMemoRow", "削除前のmemoContents=${getMemoContents().contentsList}")

            //FocusViewの下に他のViewがある場合の制約のセット。無い場合はそのままViewを削除する。
            when {
                maxIndexOfList > targetMemoRowIndexInList -> {
                    Log.d("場所:deleteMemoRow", "下に他のViewがある場合")
                    val nextMemoRowId =
                        getMemoContents().contentsList[targetMemoRowIndexInList + 1].memoRowId
                    memoRow.setConstraintForDeleteMemoRow(
                        container, MemoRowId(formerMemoRowId), nextMemoRowId
                    )
                }
            }

            formerMemoRow.apply {
                setTextAndCursorPosition(
                    Text(textOfFormerMemoRow + textOfTargetMemoRow), textOfFormerMemoRow.length
                )
                Log.d("場所:deleteMemoRow", "setSelection(${textOfFormerMemoRow.length})")
                when {
                    this.text.isNotEmpty() -> ifAtFirstInText.value = false
                }
            }

            memoRow.removeMemoRowFromLayout(executeId.fragment, container, formerMemoRow)
            deleteFromMemoContents(MemoRowId(memoRow.id))
            Queue4MemoContents.execute(this, Complete())
        }
    }

    private fun MemoRow.setEnterKeyAction(fragment: Fragment) {
        this.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                Log.d("場所:beforeTextChanged", "s=$s start=$start  count=$count after=$after")
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                Log.d("場所:onTextChanged", "s=$s start=$start before=$before count=$count")

                //フラグをfalseに更新する処理。trueにはsetBackSpaceKeyAction()の中で更新している
                ifAtFirstInText.compareAndSet(expect = true, update = false)
                Log.d("場所:onTextChanged", "atFirstInText=${ifAtFirstInText.value}")
            }

            override fun afterTextChanged(s: Editable?) {
                Log.d("場所:afterTextChanged", "s=$s")
                when {
                    s !== null && """\n""".toRegex().containsMatchIn(s.toString()) -> {
                        Log.d("場所:afterTextChanged", "改行処理に入った")
                        this@setEnterKeyAction.apply {
                            val textBringToNextRow = s.toString().substringAfter("\n")
                            setText(
                                s.toString().replace("\n" + textBringToNextRow, ""),
                                TextView.BufferType.NORMAL
                            )

                            Queue4MemoContents.execute(
                                this@MemoMainViewModel, CreateNextMemoRow(fragment, Text(textBringToNextRow))
                            )
                        }
                    }
                    else -> return
                }
            }
        } )
    }

    private fun MemoRow.setBackSpaceKeyAction(viewModel: MemoMainViewModel,
                                              executeId: CreateNextMemoRow) {
        setOnKeyListener { v, code, event ->
            when {
                code == KeyEvent.KEYCODE_DEL && viewModel.ifAtFirstInText.value -> {
                    Log.d("場所:setOnKeyListener ", "DeleteViewId=${(v as MemoRow).id}")
                    Log.d("場所:setOnKeyListener ", "text=${v.text}")
                    Log.d("場所:setOnKeyListener ", "selectionEnd=${v.selectionEnd}")
                    Log.d("場所:setOnKeyListener ", "atFirstInText=${viewModel.ifAtFirstInText.value}")

                    val targetMemoRow = container.findFocus()
                    if (targetMemoRow is MemoRow) {
                        val targetMemoRowIndexInList =
                            viewModel.getMemoRowIndexInList(MemoRowId(targetMemoRow.id))
                        Log.d("場所:setOnKeyListener ", "Ifの中に入った")
                        Log.d("場所:setOnKeyListener ", "targetMemoRow=${targetMemoRow}")
                        Log.d("場所:setOnKeyListener ", "targetMemoRowId=${targetMemoRow.id}")

                        val targetMemoRowInfo =
                            viewModel.getMemoContents().contentsList[targetMemoRowIndexInList]
                        val fragment = executeId.fragment
                        when {
                            targetMemoRowInfo.checkBoxId.value is Some ->
                                Queue4MemoContents.execute(viewModel, DeleteCheckBox(fragment, targetMemoRow))
                            targetMemoRowInfo.bulletId.value is Some ->
                                Queue4MemoContents.execute(viewModel, DeleteBullet(fragment, targetMemoRow))
                        }

                        Queue4MemoContents.execute(viewModel, DeleteMemoRow(fragment, targetMemoRow))
                    }
                }

                //フライングでDel処理に入らないように、Delキー処理の後にフラグをtrueに更新
                //falseにはsetEnterAction()の中で更新している
                this.selectionEnd == 0 && !viewModel.ifAtFirstInText.value -> {
                    viewModel.ifAtFirstInText.value = true
                    Log.d("場所:setOnKeyListener#Frag更新処理", "atFirstInText=${viewModel.ifAtFirstInText.value}")
                }
            }
            false
        }
    }

    private fun createNewMemoRow(fragment:Fragment): MemoRow {
        return EditText(fragment.context, null, 0, R.style.MemoEditTextStyle).apply {
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            )
            id = View.generateViewId()

            setEnterKeyAction(fragment)

            setOnFocusChangeListener { v, hasFocus ->
                Log.d("場所:setOnFocusChangeListener", "id=${v.id}")

                when {
                    v is MemoRow && !hasFocus -> {
                        this@MemoMainViewModel.apply {
                            val index = getMemoRowIndexInList(MemoRowId(v.id))
                            Log.d("場所:setOnFocusChangeListener", "index=$index")
                            val copiedMemoRowInfo =
                                getMemoContents().contentsList[index].copy(text = Text(v.text.toString()))
                            Log.d("場所:setOnFocusChangeListener", "copiedMemoRowInfo=$copiedMemoRowInfo")
                            modifyMemoRowInfo(v, copiedMemoRowInfo)
                        }
                    }
                }
            }
        }
    }


    private fun addCheckBox(executeId: AddCheckBox) {
        Log.d("場所:addCheckBox", "checkBox追加処理に入った")
        this.apply {
            val memoRow = executeId.memoRow
            val container = memoRow.parent
            val newCheckBox = CheckBox(executeId.fragment.context).apply {
                layoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                )
                ViewGroup.MarginLayoutParams(0, 0)
                id = View.generateViewId()
                textSize = 0f
                setPadding(4)
                setOnCheckedChangeListener { buttonView, isChecked ->
                    when (isChecked) {
                        true -> {
                            val index = getMemoRowIndexInList(MemoRowId(memoRow.id))
                            val copiedMemoRowInfo = getMemoContents()
                                .contentsList[index].copy(checkBoxState = CheckBoxState(true))

                            modifyMemoRowInfo(memoRow, copiedMemoRowInfo)
                            memoRow.setTextColor(Color.GRAY)
                        }
                        false -> {
                            val index = getMemoRowIndexInList(MemoRowId(memoRow.id))
                            val copiedMemoRowInfo = getMemoContents()
                                .contentsList[index].copy(checkBoxState = CheckBoxState(false))

                            modifyMemoRowInfo(memoRow, copiedMemoRowInfo)
                            memoRow.setTextColor(Color.BLACK)
                        }
                    }
                    Log.d(
                        "場所:OnCheckedChangeListener",
                        """targetMemoRowId=${this.id}、
                    checkBoxState=${getMemoContents().contentsList[getMemoRowIndexInList(
                            MemoRowId(memoRow.id))].checkBoxState.value}
                    textColor=${memoRow.textColors}"""
                    )
                }
            }

            if (container is ConstraintLayout) {
                container.addView(newCheckBox)
                memoRow.setConstraintForOptView(container, newCheckBox, 80)
            }

            val index = getMemoRowIndexInList(MemoRowId(memoRow.id))
            val copiedMemoRowInfo = getMemoContents().contentsList[index].copy(
                checkBoxId = CheckBoxId(Some(newCheckBox.id)), checkBoxState = CheckBoxState(false)
            )
            modifyMemoRowInfo(memoRow, copiedMemoRowInfo)

            Queue4MemoContents.execute(this, Complete())
        }
    }

    private fun deleteCheckBox(executeId: DeleteCheckBox) {
        Log.d("場所:deleteCheckBox", "checkBox削除処理に入った")
        this.apply {
            val fragment = executeId.fragment
            val memoRow = executeId.memoRow
            val container = memoRow.parent
            val checkBoxId =
                getMemoContents().contentsList[getMemoRowIndexInList(MemoRowId(memoRow.id))].checkBoxId

            memoRow.apply {
                if (container is ConstraintLayout) {
                    setConstraintForDeleteOptView(container)
                    removeOptViewFromLayout(fragment, container, checkBoxId)
                }
            }

            val index = getMemoRowIndexInList(MemoRowId(memoRow.id))
            val copiedMemoRowInfo = getMemoContents().contentsList[index].copy(
                checkBoxId = CheckBoxId(None), checkBoxState = CheckBoxState(false)
            )
            modifyMemoRowInfo(memoRow, copiedMemoRowInfo)

            Queue4MemoContents.execute(this, Complete())
        }
    }


    private fun addBullet(executeId: AddBullet) {
        Log.d("場所:addBullet", "bullet追加処理に入った")
        this.apply {
            val memoRow = executeId.memoRow
            val container = memoRow.parent
            val newBullet = TextView(executeId.fragment.context).apply {
                layoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                )
                id = View.generateViewId()
                setPadding(4)
                setBackgroundResource(R.color.colorTransparent)
                text = "・"
            }

            if (container is ConstraintLayout) {
                container.addView(newBullet)
                memoRow.setConstraintForOptView(container, newBullet, 50, 20)
            }

            val index = getMemoRowIndexInList(MemoRowId(memoRow.id))
            val copiedMemoRowInfo =
                getMemoContents().contentsList[index].copy(bulletId = BulletId(Some(newBullet.id)))
            modifyMemoRowInfo(memoRow, copiedMemoRowInfo)

            Queue4MemoContents.execute(this, Complete())
        }
    }

    private fun deleteBullet(executeId: DeleteBullet) {
        Log.d("場所:deleteBullet", "bulletの削除処理に入った")
        this.apply {
            val memoRow = executeId.memoRow
            val bulletId =
                getMemoContents().contentsList[getMemoRowIndexInList(MemoRowId(memoRow.id))].bulletId

            memoRow.apply {
                setConstraintForDeleteOptView(container)
                removeOptViewFromLayout(executeId.fragment, container, bulletId)
            }

            val index = getMemoRowIndexInList(MemoRowId(memoRow.id))
            val copiedMemoRowInfo =
                getMemoContents().contentsList[index].copy(bulletId = BulletId(None))
            modifyMemoRowInfo(memoRow, copiedMemoRowInfo)

            Queue4MemoContents.execute(this, Complete())
        }
    }

    private fun clearAllInMemoContents() {
        Log.d("場所:clearAllInMemoContents", "ClearAll処理に入った")
        memoContents = MemoContents(listOf<MemoRowInfo>().k())
        ifAtFirstInText.value = true

        Queue4MemoContents.execute(this, Complete())
    }




    override fun onCleared() {
        super.onCleared()
        //ここに終了処理を書く
    }
}

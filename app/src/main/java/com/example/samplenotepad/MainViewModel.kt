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


const val COMPLETE = 0
const val CREATE_FIRST_MEMO_ROW = 1
const val CREATE_NEXT_MEMO_ROW = 2
const val DELETE_MEMO_ROW = 3
const val ADD_CHECKBOX = 4
const val DELETE_CHECKBOX = 5
const val ADD_BULLET = 6
const val DELETE_BULLET = 7
const val CLEAR_ALL = 8


class MainViewModel : ViewModel() {

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
        private val queue = mutableListOf<Triple<Int, MemoRow, Text>>()
        //trueの時のみmemoContentsの変更処理を許可するトランザクション処理のためのフラグ
        private val flag4MemoContents = AtomicBooleanW(true)


        private fun push(functionId: Int, memoRow: MemoRow, text: Text) {
            val value = Triple(functionId, memoRow, text)
            queue.add(value)
        }

        private fun pop() = queue.drop(1)

        internal fun execute(fragment: Fragment, viewModel: MainViewModel,
                             functionId: Int, memoRow:MemoRow, text: Text) {
            when (flag4MemoContents.compareAndSet(expect = true, update = false)) {
                true -> {
                    Log.d("場所:Queue4MemoContents#excute", "flag4MemoContentsがtrueの場合")
                    when (functionId) {
                        CREATE_FIRST_MEMO_ROW -> viewModel.createFirstMemoRow(fragment, viewModel, text)
                        CREATE_NEXT_MEMO_ROW -> viewModel.createNextMemoRow(fragment, viewModel, text)
                        DELETE_MEMO_ROW -> viewModel.deleteMemoRow(fragment, viewModel, memoRow)
                        ADD_CHECKBOX -> viewModel.addCheckBox(fragment, viewModel, memoRow)
                        DELETE_CHECKBOX -> viewModel.deleteCheckBox(fragment, viewModel, memoRow)
                        ADD_BULLET -> viewModel.addBullet(fragment, viewModel, memoRow)
                        DELETE_BULLET -> viewModel.deleteBullet(fragment, viewModel, memoRow)
                        CLEAR_ALL -> viewModel.clearAllInMemoContents(fragment, viewModel, memoRow)
                        COMPLETE -> return
                    }
                }
                false -> {
                    Log.d("場所:Queue4MemoContents#excute", "flag4MemoContentsがfalseの場合")
                    when {
                        functionId == COMPLETE && queue.isNotEmpty() -> {
                            Log.d("場所:Queue4MemoContents#excute",
                                "flag4MemoContentsがfalseで実行中だった処理が終了した場合")
                            val value = queue[0]

                            pop()
                            flag4MemoContents.value = true
                            when (value.first) {
                                CREATE_FIRST_MEMO_ROW ->
                                    viewModel.createFirstMemoRow(fragment, viewModel, value.third)
                                CREATE_NEXT_MEMO_ROW ->
                                    viewModel.createNextMemoRow(fragment, viewModel, value.third)
                                DELETE_MEMO_ROW -> viewModel.deleteMemoRow(fragment, viewModel, value.second)
                                ADD_CHECKBOX -> viewModel.addCheckBox(fragment, viewModel, value.second)
                                DELETE_CHECKBOX -> viewModel.deleteCheckBox(fragment, viewModel, value.second)
                                ADD_BULLET -> viewModel.addBullet(fragment, viewModel, value.second)
                                DELETE_BULLET -> viewModel.deleteBullet(fragment, viewModel, value.second)
                                CLEAR_ALL -> viewModel.clearAllInMemoContents(fragment, viewModel, memoRow)
                            }

                        }
                        functionId == COMPLETE && queue.isEmpty() -> {
                            Log.d("場所:Queue4MemoContents#excute",
                                "flag4MemoContentsがfalseで実行中だった処理が終了し、且つQueueが空の場合")
                            flag4MemoContents.value = true
                        }
                        functionId == CREATE_FIRST_MEMO_ROW -> {
                            Log.d("場所:Queue4MemoContents#excute", "flag4MemoContentsがfalseでqueueされる場合")
                            push(functionId, memoRow, text)
                        }
                        functionId == CREATE_NEXT_MEMO_ROW -> push(functionId, memoRow, text)
                        functionId == DELETE_MEMO_ROW -> push(functionId, memoRow, text)
                        functionId == ADD_CHECKBOX -> push(functionId, memoRow, text)
                        functionId == DELETE_CHECKBOX -> push(functionId, memoRow, text)
                        functionId == ADD_BULLET -> push(functionId, memoRow, text)
                        functionId == DELETE_BULLET -> push(functionId, memoRow, text)
                        functionId == CLEAR_ALL -> push(functionId, memoRow, text)
                        else -> return
                    }
                }
            }
        }
    }

    //メモコンテンツの最初の行をセットする
    private fun createFirstMemoRow(fragment: Fragment, viewModel: MainViewModel, text: Text) {
        viewModel.apply {
            container = fragment.requireActivity().findViewById(R.id.memoContentsContainerLayout)

            val newMemoRow = createNewMemoRow(fragment, viewModel).apply {
                setConstraintForFirstMemoRow(container)
                setTextAndCursorPosition(text, text.value.length)
            }
            Log.d("場所:createFirstMemoRow", "newMemoRowId=${newMemoRow.id}")

            addToMemoContents(0, MemoRowInfo(MemoRowId(newMemoRow.id), text))
            Log.d("場所:createFirstMemoRow", "memoContents=${getMemoContents()}")

            Queue4MemoContents.execute(fragment, viewModel, COMPLETE, newMemoRow, Text(""))
        }
    }

    private fun createNextMemoRow(fragment: Fragment, viewModel: MainViewModel, text: Text) {
        viewModel.apply {
            val targetMemoRowId = container.findFocus().id
            val targetMemoRowIndexInList = getMemoRowIndexInList(MemoRowId(targetMemoRowId))
            val maxIndexOfList = getMemoContents().contentsList.size -1
            val newMemoRow = createNewMemoRow(fragment, viewModel).apply {
                setBackSpaceKeyAction(fragment, viewModel)
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

                setTextAndCursorPosition(text)
                addToMemoContents(targetMemoRowIndexInList + 1, MemoRowInfo(MemoRowId(newMemoRow.id)))
            }

            Queue4MemoContents.execute(fragment, viewModel, COMPLETE, newMemoRow, Text(""))
        }
    }

    private fun deleteMemoRow(fragment: Fragment, viewModel:MainViewModel, memoRow:MemoRow) {
        viewModel.apply {
            val targetMemoRowIndexInList = getMemoRowIndexInList(MemoRowId(memoRow.id))
            val maxIndexOfList = getMemoContents().contentsList.size - 1
            val formerMemoRowId =
                getMemoContents().contentsList[targetMemoRowIndexInList - 1].memoRowId.value
            val formerMemoRow = fragment.requireActivity().findViewById<EditText>(formerMemoRowId)
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

            memoRow.removeMemoRowFromLayout(fragment, container, formerMemoRow)
            deleteFromMemoContents(MemoRowId(memoRow.id))
            Queue4MemoContents.execute(fragment, viewModel, COMPLETE, memoRow, Text(""))
        }
    }

    private fun MemoRow.setEnterKeyAction(fragment: Fragment, viewModel: MainViewModel) {
        this.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                Log.d("場所:beforeTextChanged", "s=$s start=$start  count=$count after=$after")
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                Log.d("場所:onTextChanged", "s=$s start=$start before=$before count=$count")

                //フラグをfalseに更新する処理。trueにはsetBackSpaceKeyAction()の中で更新している
                viewModel.ifAtFirstInText.compareAndSet(expect = true, update = false)
                Log.d("場所:onTextChanged", "atFirstInText=${viewModel.ifAtFirstInText.value}")
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
                                fragment, viewModel, CREATE_NEXT_MEMO_ROW, this, Text(textBringToNextRow)
                            )
                        }
                    }
                    else -> return
                }
            }
        } )
    }

    private fun MemoRow.setBackSpaceKeyAction(fragment: Fragment, viewModel: MainViewModel) {
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
                        when {
                            targetMemoRowInfo.checkBoxId.value is Some ->
                                Queue4MemoContents.execute(
                                    fragment, viewModel, DELETE_CHECKBOX, targetMemoRow, Text("")
                                )
                            targetMemoRowInfo.bulletId.value is Some ->
                                Queue4MemoContents.execute(
                                    fragment, viewModel, DELETE_BULLET, targetMemoRow, Text("")
                                )
                        }

                        Queue4MemoContents.execute(fragment, viewModel, DELETE_MEMO_ROW, this, Text(""))
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

    private fun createNewMemoRow(fragment: Fragment, viewModel: MainViewModel): MemoRow {
        return EditText(fragment.context, null, 0, R.style.MemoEditTextStyle).apply {
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            )
            id = View.generateViewId()

            setEnterKeyAction(fragment, viewModel)

            setOnFocusChangeListener { v, hasFocus ->
                Log.d("場所:setOnFocusChangeListener", "id=${v.id}")

                when {
                    v is MemoRow && !hasFocus -> {
                        viewModel.apply {
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


    private fun addCheckBox(fragment: Fragment, viewModel: MainViewModel, memoRow: MemoRow) {
        Log.d("場所:addCheckBox", "checkBox追加処理に入った")
        viewModel.apply {
            val container = memoRow.parent
            val newCheckBox = CheckBox(fragment.context).apply {
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
                    checkBoxState=${getMemoContents().contentsList[viewModel.getMemoRowIndexInList(
                            MemoRowId(memoRow.id)
                        )].checkBoxState.value}
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

            Queue4MemoContents.execute(fragment, viewModel, COMPLETE, memoRow, Text(""))
        }
    }

    private fun deleteCheckBox(fragment: Fragment, viewModel: MainViewModel, memoRow: MemoRow) {
        Log.d("場所:deleteCheckBox", "checkBox削除処理に入った")
        viewModel.apply {
            val container = memoRow.parent
            val checkBoxId = viewModel.getMemoContents().contentsList[
                    viewModel.getMemoRowIndexInList(MemoRowId(memoRow.id))
            ].checkBoxId

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

            Queue4MemoContents.execute(fragment, viewModel, COMPLETE, memoRow, Text(""))
        }
    }


    private fun addBullet(fragment: Fragment, viewModel: MainViewModel, memoRow: MemoRow) {
        Log.d("場所:addBullet", "bullet追加処理に入った")
        viewModel.apply {
            val container = memoRow.parent
            val newBullet = TextView(fragment.context).apply {
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

            Queue4MemoContents.execute(fragment, viewModel, COMPLETE, memoRow, Text(""))
        }
    }

    private fun deleteBullet(fragment: Fragment, viewModel: MainViewModel, memoRow:MemoRow) {
        viewModel.apply {
            val container = memoRow.parent
            val bulletId = viewModel.getMemoContents().contentsList[
                    viewModel.getMemoRowIndexInList(MemoRowId(memoRow.id))
            ].bulletId

            memoRow.apply {
                if (container is ConstraintLayout) {
                    setConstraintForDeleteOptView(container)
                    removeOptViewFromLayout(fragment, container, bulletId)
                }
            }

            val index = getMemoRowIndexInList(MemoRowId(memoRow.id))
            val copiedMemoRowInfo =
                getMemoContents().contentsList[index].copy(bulletId = BulletId(None))
            modifyMemoRowInfo(memoRow, copiedMemoRowInfo)

            Queue4MemoContents.execute(fragment, viewModel, COMPLETE, memoRow, Text(""))
        }
    }

    private fun clearAllInMemoContents(fragment: Fragment, viewModel: MainViewModel, memoRow: MemoRow) {
        memoContents = MemoContents(listOf<MemoRowInfo>().k())
        viewModel.ifAtFirstInText.value = true

        Queue4MemoContents.execute(fragment, viewModel, COMPLETE, memoRow, Text(""))
    }




    override fun onCleared() {
        super.onCleared()
        //ここに終了処理を書く
    }
}
